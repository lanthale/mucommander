package com.mucommander.commons.file.impl.vsphere;

import com.mucommander.commons.file.*;
import com.mucommander.commons.file.connection.ConnectionHandler;
import com.mucommander.commons.file.connection.ConnectionHandlerFactory;
import com.mucommander.commons.file.connection.ConnectionPool;
import com.mucommander.commons.file.util.PathUtils;
import com.mucommander.commons.io.FileTransferException;
import com.mucommander.commons.io.RandomAccessInputStream;
import com.mucommander.commons.io.RandomAccessOutputStream;
import com.mucommander.commons.io.StreamUtils;
import com.vmware.vim25.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HttpsURLConnection;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.ws.soap.SOAPFaultException;
import java.io.*;
import java.net.*;
import java.rmi.RemoteException;
import java.util.*;

/**
 * VSphereFile provides access to files on virtual machines on vSphere 5+
 * servers.
 * 
 * <p>
 * The associated {@link FileURL} scheme is {@link FileProtocols#VSPHERE}. The
 * host part of the URL designates the vSphere server. Credentials must be
 * specified in the login and password parts as vSphere servers require a login
 * and password. The path separator is '/', even on windows virtual machines.
 * 
 * The first path of the path is the identifier of the guest VM (see below) we
 * want to access. That part may also contain username and password for the
 * guest VMs - though it is less secure, so it is better to use the GUI dialog.
 * 
 * Note that the machine credentials cab also be set as a property named
 * "guestCredentials" of this object.
 * 
 * The identifier of a VM can be one of the following: - Instance UUID - BIOS
 * UUID - IP address
 * 
 * Note that even if you use IP to identify the machine, you don't need network
 * connectivity for accessing the machine. all access is done via vSphere APIs.
 * 
 * <p>
 * Here are a few examples of valid vSphere URLs: <code>
 * vsphere://vsphere5-server/501fc8db-f9dc-562b-6310-3c6b7ace2377/C:<br>
 * vsphere://admin:pass@vsphere5-server/501fc8db-f9dc-562b-6310-3c6b7ace2377/C:<br>
 * vsphere://admin:pass@vsphere5-server/guestadmin:guestpass@501fc8db-f9dc-562b-6310-3c6b7ace2377/C:<br>
 * </code>
 * 
 * <p>
 * Access to vSphere files is provided by the <code>vim25</code> library
 * distributed under the VMware Software Development Kit (SDK) License
 * Agreement. See: http://www.vmware.com/go/vwssdk-redistribution-info
 * 
 * @see ConnectionPool
 * @author Yuval Kohavi, yuval.kohavi@intigua.com
 */
public class VSphereFile extends ProtocolFile implements
		ConnectionHandlerFactory {
	private static final Logger LOGGER = LoggerFactory
			.getLogger(VSphereFile.class);

	public static final String GUEST_CREDENTIALS = "guestCredentials";

	private static final String SEPARATOR = "/";

	private String guestPass;

	private String guestUser;

	private String vmIdentifier;

	private String pathInsideVm;

	private ManagedObjectReference vm;

	private NamePasswordAuthentication credentials;

	private ManagedObjectReference guestOperationsManager;

	private long size = -1;

	private boolean isFile;

	private boolean isSymLink;

	private boolean isDir;

	private long date;

	private VSphereFile parent;

	private String guestOsId;

	public VSphereFile(FileURL url) throws IOException {
		super(url);
		VsphereConnHandler connHandler = null;
		try {
			setPath(url.getPath());

			connHandler = getConnHandler();
			guestOperationsManager = connHandler.getClient()
					.getServiceContent().getGuestOperationsManager();

			getMor(connHandler);
			fixPathInVmIfNeeded(connHandler);

			checkAttributes(connHandler);
		} catch (RuntimeFaultFaultMsg | FileFaultFaultMsg | GuestOperationsFaultFaultMsg | TaskInProgressFaultMsg |
				InvalidPropertyFaultMsg | URISyntaxException | InvalidStateFaultMsg e) {
			translateandLogException(e);
		} finally {
			releaseConnHandler(connHandler);
		}

	}

	private void releaseConnHandler(VsphereConnHandler connHandler) {
		if (connHandler != null) {
			connHandler.releaseLock();
		}
	}

	private VSphereFile(FileURL url, VSphereFile related)
			throws URISyntaxException, IOException, RuntimeFaultFaultMsg,
			InvalidPropertyFaultMsg, FileFaultFaultMsg,
			GuestOperationsFaultFaultMsg, InvalidStateFaultMsg,
			TaskInProgressFaultMsg {
		super(url);
		setPath(url.getPath());

		this.vm = related.vm;
		this.guestOsId = related.guestOsId;
		this.guestOperationsManager = related.guestOperationsManager;

		fixPathInVmIfNeeded(null);

		VsphereConnHandler connHandler = null;
		try {
			connHandler = getConnHandler();
			checkAttributes(connHandler);
		} finally {
			releaseConnHandler(connHandler);
		}
	}

	public VSphereFile(FileURL url, VSphereFile parent,
			GuestFileInfo guestFileInfo) throws RuntimeFaultFaultMsg,
			IOException,
			InvalidPropertyFaultMsg, URISyntaxException {
		super(url);
		setPath(url.getPath());

		this.parent = parent;
		this.vm = parent.vm;
		this.guestOsId = parent.guestOsId;
		this.guestOperationsManager = parent.guestOperationsManager;

		fixPathInVmIfNeeded(null);

		updateAttributes(guestFileInfo);

	}

	private void getMor(VsphereConnHandler connHandler)
			throws RuntimeFaultFaultMsg {
		vm = connHandler.getClient().findVmByIp(vmIdentifier);
		if (vm == null) {
			vm = connHandler.getClient().findVmByUuid(vmIdentifier, true);
		}
		if (vm == null) {
			vm = connHandler.getClient().findVmByUuid(vmIdentifier, false);
		}

		if (vm == null) {
			throw new IllegalArgumentException("Machine identifier "
					+ vmIdentifier + " not found.");
		}
	}

	private void fixPathInVmIfNeeded(VsphereConnHandler connHandler)
			throws RuntimeFaultFaultMsg, RemoteException, InvalidPropertyFaultMsg {

		if (this.guestOsId == null) {
			if (connHandler != null) {
				this.guestOsId = (String) connHandler.getClient()
						.getProperties(vm, "config.guestId")[0];
			}
		}

		boolean isWin = false;
		if (this.guestOsId != null) {
			// is it a windows machine ?
			// see possible values for guest id here:
			// http://www.vmware.com/support/developer/vc-sdk/visdk41pubs/ApiReference/vim.vm.GuestOsDescriptor.GuestOsIdentifier.html
			isWin = guestOsId.startsWith("win");
		}

		if (pathInsideVm.isEmpty()) {

			if (isWin) {
				// we assume that "C:" is a good default for windows
				pathInsideVm = "C:";
			} else {
				pathInsideVm = "/";
			}

			// set the url to reflect the path inside the vm
			fileURL.setPath(fileURL.getPath() + pathInsideVm);
		}
	}

	private void setPath(String path) throws URISyntaxException, IOException {
		// path starts with a /

		// get first component:
		int index = path.indexOf('/', 1);
		String first = path.substring(1, index);
		String rest = path.substring(index + 1);

		// the first part of the path is very similar to a url, due to the fact
		// it
		// may contain guest credentials. So I use the URI class to help me
		// parse it.
		// first as a url of its own. http = dymm
		URI url2 = new URI("dummy://" + first);
		String uinfo;
		credentials = new NamePasswordAuthentication();
		vmIdentifier = url2.getHost();

		String guestCred = fileURL.getProperty(GUEST_CREDENTIALS);
		if ((guestCred != null) && (!guestCred.isEmpty())) {
			uinfo = guestCred;
		} else {
			uinfo = url2.getUserInfo();
		}

		if (uinfo == null) {
			throw new IOException(
					"No guest credentials provided. please start the connection from the UI");
		}

		int indexOf = uinfo.indexOf(":");
		if (indexOf == -1) {
			throw new IOException(
					"No guest credentials provided. please start the connection from the UI");
		}

		guestUser = uinfo.substring(0, indexOf);
		guestPass = uinfo.substring(indexOf + 1);
		credentials.setInteractiveSession(false);
		credentials.setUsername(guestUser);
		credentials.setPassword(guestPass);

		pathInsideVm = rest;

	}

	private ManagedObjectReference getFileManager(VsphereConnHandler connHandler)
			throws RemoteException, InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {
		ManagedObjectReference fileManager = (ManagedObjectReference) connHandler
				.getClient().getProperties(guestOperationsManager,
						"fileManager")[0];
		return fileManager;
	}

	private void checkAttributes(VsphereConnHandler connHandler)
			throws IOException, FileFaultFaultMsg,
			GuestOperationsFaultFaultMsg, InvalidStateFaultMsg,
			RuntimeFaultFaultMsg, TaskInProgressFaultMsg,
			InvalidPropertyFaultMsg {

		ManagedObjectReference fileManager = getFileManager(connHandler);
		GuestListFileInfo res;
		try {
			res = connHandler
					.getClient()
					.getVimPort()
					.listFilesInGuest(fileManager, vm, credentials,
							getPathInVm(), null, null, null);
		} catch (SOAPFaultException e) {
			if (isFileNotFound(e)) {
				return;
			}
			throw e;
		}
		if (res.getFiles().size() == 1) {
			// only one result - it's a file
			GuestFileInfo guestFileInfo = res.getFiles().get(0);

			updateAttributes(guestFileInfo);
		} else {
			// more than one result - it's a directory.
			// find the entry for "."
			for (GuestFileInfo f : res.getFiles()) {
				if (f.getPath().equals(".")) {
					updateAttributes(f);
					break;
				}
			}
		}

	}

	private boolean isFileNotFound(SOAPFaultException e) {
//		Detail detail = e.getFault().getDetail();
//		NodeList childNodes = detail.getChildNodes();
//
//		for (int i = 0; i < childNodes.getLength(); ++i) {
//			// I hate soap...
//			if (childNodes.item(i).getNodeName().equals("FileNotFoundFault")) {
//				return true;
//			}
//		}
		return false;
	}

	void updateAttributes(GuestFileInfo guestFileInfo) {
		if (guestFileInfo.getType().equals(GuestFileType.FILE.value())) {
			isFile = true;
			size = guestFileInfo.getSize();
		} else if (guestFileInfo.getType().equals(GuestFileType.SYMLINK.value())) {
			isSymLink = true;
		} else if (guestFileInfo.getType().equals(GuestFileType.DIRECTORY.value())) {
			isDir = true;
		}
		date = guestFileInfo.getAttributes().getModificationTime().toGregorianCalendar().getTimeInMillis();

	}

	private VsphereConnHandler getConnHandler() throws IOException {
		VsphereConnHandler connHandler = (VsphereConnHandler) ConnectionPool
				.getConnectionHandler(this, fileURL, true);
		try {
			connHandler.checkConnection();
		} catch (RuntimeException | IOException e) {
			releaseConnHandler(connHandler);
			throw e;
		}
		return connHandler;
	}

	@Override
	public ConnectionHandler createConnectionHandler(FileURL location) {
		return new VsphereConnHandler(location);
	}

	@Override
	public long getLastModifiedDate() {
		return date;
	}

	@Override
	public void setLastModifiedDate(long lastModified) throws IOException {

		VsphereConnHandler connHandler = null;
		try {
			GuestFileAttributes gfa = new GuestFileAttributes();
			gfa.setModificationTime(getTimeToXmlTime(lastModified));
			connHandler = getConnHandler();
			connHandler.getClient().getVimPort().changeFileAttributesInGuest(
					getFileManager(connHandler), vm, credentials, getPathInVm(), gfa);
		} catch (FileFaultFaultMsg | RuntimeFaultFaultMsg | GuestOperationsFaultFaultMsg | InvalidStateFaultMsg | TaskInProgressFaultMsg | InvalidPropertyFaultMsg | DatatypeConfigurationException e) {
			translateandLogException(e);
		} finally {
			releaseConnHandler(connHandler);
		}

	}

	private XMLGregorianCalendar getTimeToXmlTime(long lastModified) throws DatatypeConfigurationException {
		GregorianCalendar gc = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
		gc.setTime(new Date(lastModified));
		return DatatypeFactory.newInstance().newXMLGregorianCalendar(gc);
	}

	@Override
	public long getSize() {
		return size;
	}

	@Override
	public AbstractFile getParent() {
		if (parent != null) {
			return parent;
		}
		String rootPath = getRootPath();

		if (rootPath.equals(pathInsideVm) || pathInsideVm.equals("/")) {
			return null;
		}

		try {
			parent = new VSphereFile(fileURL.getParent(), this);
		} catch (URISyntaxException | IOException | InvalidPropertyFaultMsg | RuntimeFaultFaultMsg | InvalidStateFaultMsg
				| TaskInProgressFaultMsg | FileFaultFaultMsg | GuestOperationsFaultFaultMsg e) {
			return null;
		}
		return parent;
	}

	@Override
	public void setParent(AbstractFile parent) {
		this.parent = (VSphereFile) parent;
	}

	@Override
	public boolean exists() {
		return isDir || isFile || isSymLink;
	}

	@Override
	public FilePermissions getPermissions() {
		return isDir ? FilePermissions.DEFAULT_DIRECTORY_PERMISSIONS
				: FilePermissions.DEFAULT_FILE_PERMISSIONS;
	}

	@Override
	public PermissionBits getChangeablePermissions() {
		return PermissionBits.EMPTY_PERMISSION_BITS;
	}

	@Override
	@UnsupportedFileOperation
	public void changePermission(int access, int permission, boolean enabled)
			throws IOException {
		throw new UnsupportedFileOperationException(
				FileOperation.CHANGE_PERMISSION);
	}

	@Override
	public String getOwner() {
		return null;
	}

	@Override
	public boolean canGetOwner() {
		return false;
	}

	@Override
	public String getGroup() {
		return null;
	}

	@Override
	public boolean canGetGroup() {
		return false;
	}

	@Override
	public boolean isDirectory() {
		return isDir;
	}

	@Override
	public boolean isSymlink() {
		return isSymLink;
	}

	@Override
    public boolean isSystem() {
        return false;
    }

	@Override
	public AbstractFile[] ls() throws IOException {
		List<GuestFileInfo> fileInfos = new ArrayList<>();
		int index = 0;
		VsphereConnHandler connHandler = null;
		try {

			connHandler = getConnHandler();

			ManagedObjectReference fileManager = getFileManager(connHandler);
			boolean haveRemaining;
			do {
				GuestListFileInfo res = connHandler
						.getClient()
						.getVimPort()
						.listFilesInGuest(fileManager, vm, credentials,
								getPathInVm(), index, null, null);
				haveRemaining = (res.getRemaining() != 0);

				fileInfos.addAll(res.getFiles());
				index = fileInfos.size();
			} while (haveRemaining);

			String parentPath = PathUtils.removeTrailingSeparator(fileURL
					.getPath()) + SEPARATOR;

			Collection<AbstractFile> res = new ArrayList<>();
			for (GuestFileInfo f : fileInfos) {
				final String name = getFileName(f.getPath());
				if (name.equals(".") || name.equals("..")) {
					continue;
				}

				FileURL childURL = (FileURL) fileURL.clone();
				childURL.setPath(parentPath + name);

				AbstractFile newFile = new VSphereFile(childURL, this, f);
				res.add(newFile);
			}
			return res.toArray(new AbstractFile[0]);
		} catch (FileFaultFaultMsg | GuestOperationsFaultFaultMsg | InvalidStateFaultMsg | RuntimeFaultFaultMsg | TaskInProgressFaultMsg | InvalidPropertyFaultMsg | URISyntaxException e) {
			translateandLogException(e);
		} finally {
			releaseConnHandler(connHandler);
		}
		// we never get here..
		return null;
	}

	private String getFileName(String path) {
		// don't search for the slash as the last character.
		int lastIndex = path.length() - 2;
		// find the last "\\" or "/"
		int index = Math.max(path.lastIndexOf('\\', lastIndex),
				path.lastIndexOf('/', lastIndex));
		if (index == -1) {
			return path;
		}

		return path.substring(index + 1);
	}

	@Override
	public void mkdir() throws IOException {
		VsphereConnHandler connHandler = null;
		try {
			connHandler = getConnHandler();
			connHandler
					.getClient()
					.getVimPort()
					.makeDirectoryInGuest(getFileManager(connHandler), vm,
							credentials, getPathInVm(), false);
		} catch (FileFaultFaultMsg | GuestOperationsFaultFaultMsg | RuntimeFaultFaultMsg | InvalidStateFaultMsg | InvalidPropertyFaultMsg | TaskInProgressFaultMsg e) {
			translateandLogException(e);
		} finally {
			releaseConnHandler(connHandler);
		}
	}

	@Override
	public InputStream getInputStream() throws IOException {
		VsphereConnHandler connHandler = null;
		try {
			connHandler = getConnHandler();
			ManagedObjectReference fileManager = getFileManager(connHandler);

			FileTransferInformation fileDlInfo = connHandler
					.getClient()
					.getVimPort()
					.initiateFileTransferFromGuest(fileManager, vm,
							credentials, getPathInVm());
			String fileDlUrl = fileDlInfo.getUrl().replace("*",
					connHandler.getClient().getServer());

			// http://stackoverflow.com/questions/921262/how-to-download-and-save-a-file-from-internet-using-java
			URL website = new URL(fileDlUrl);
			return website.openStream();

		} catch (InvalidPropertyFaultMsg | RuntimeFaultFaultMsg | GuestOperationsFaultFaultMsg | FileFaultFaultMsg | TaskInProgressFaultMsg | InvalidStateFaultMsg e) {
			translateandLogException(e);
		} finally {
			releaseConnHandler(connHandler);
		}
		return null;

	}

	private String getPathInVm() {
		if (isWinPath()) {
			return pathInsideVm.replace("/", "\\");
		}
		return pathInsideVm;
	}

	@Override
	public void copyStream(InputStream in, boolean append, long length) throws FileTransferException {
		if (append || length == -1) {
			super.copyStream(in, append, length);
		} else {
			try {
				doCopyRemoteFileName(in, length);
			} catch (IOException e) {
				LOGGER.error("Failed copying stream", e);
				throw new FileTransferException(
						FileTransferException.UNKNOWN_REASON);

			}
		}
	}

	private void doCopyRemoteFileName(InputStream in, long length)
			throws IOException {
		VsphereConnHandler connHandler = null;
		try {
			connHandler = getConnHandler();
			ManagedObjectReference fileManager = getFileManager(connHandler);

			copyFileToRemote(getPathInVm(), in, length, connHandler,
					fileManager);
		} catch (InvalidPropertyFaultMsg | RuntimeFaultFaultMsg | IOException e) {
			translateandLogException(e);
		} finally {
			releaseConnHandler(connHandler);
		}
	}

	/**
	 * vSphere APIs require the file size when copying a file.
	 * 
	 * mucommander enables file copy with known size, and also with unknown
	 * size. This class supports the unknown size use case. It saves all the
	 * data to a temp file and copies it on close, when its size is known.
	 * 
	 */
	public class VSphereOutputStream extends FileOutputStream {

		private ManagedObjectReference fileManager;
		private VsphereConnHandler connHandler;
		private String fileName;
		private File tmpFile;

		public VSphereOutputStream(VsphereConnHandler connHandler,
				ManagedObjectReference fileManager, ManagedObjectReference vm,
				NamePasswordAuthentication credentials, String fileName)
				throws IOException {
			this(connHandler, fileManager, vm, credentials, File
					.createTempFile("tmpVixCopy", ".tmp"), fileName);
		}

		private VSphereOutputStream(VsphereConnHandler connHandler,
				ManagedObjectReference fileManager, ManagedObjectReference vm,
				NamePasswordAuthentication credentials, File tmpFile,
				String fileName) throws IOException {
			super(tmpFile);
			this.tmpFile = tmpFile;
			this.connHandler = connHandler;
			this.fileManager = fileManager;
			this.fileName = fileName;
		}

		@Override
		public void close() throws IOException {
			if (connHandler == null) {
				return;
			}
			super.close();
			try (InputStream in = new FileInputStream(tmpFile)) {
				copyFileToRemote(fileName, in, this.tmpFile.length(), connHandler, fileManager);
			} finally {
				connHandler.releaseLock();
				connHandler = null;

				tmpFile.delete();
			}
		}
	}

	private void copyFileToRemote(String fileName, InputStream in, long length,
			VsphereConnHandler connHandler, ManagedObjectReference fileManager)	throws IOException {
		try {
			String fileUploadUrl = getFileUploadUrl(fileName, length, connHandler, fileManager);
			URLConnection conn = prepareConnection(fileUploadUrl, length);

			sendFile(in, conn);

			parseResponse(conn);

		} catch (RuntimeFaultFaultMsg | GuestOperationsFaultFaultMsg | FileFaultFaultMsg
				| TaskInProgressFaultMsg | InvalidStateFaultMsg e) {
			translateandLogException(e);
		}

	}

	private void parseResponse(URLConnection conn) throws IOException {
		int responseCode = 0;
		String message = "";
		if (conn instanceof HttpURLConnection) {
			responseCode = ((HttpURLConnection) conn).getResponseCode();
			message = ((HttpsURLConnection) conn).getResponseMessage();
		}

		if (responseCode != 200) {
			throw new IOException("Failed to copy file using vsphere: " + message);
		}
	}

	private void sendFile(InputStream in, URLConnection conn) throws IOException {
		conn.connect();

		try (OutputStream out = conn.getOutputStream()) {
			StreamUtils.copyStream(in, out, IO_BUFFER_SIZE);
		}
	}

	private URLConnection prepareConnection(String fileUploadUrl, long fileSize)
			throws TaskInProgressFaultMsg, IOException {

		// http://stackoverflow.com/questions/3386832/upload-a-file-using-http-put-in-java
		URL url = new URL(fileUploadUrl);
		URLConnection conn = url.openConnection();
		conn.setDoInput(true);
		conn.setDoOutput(true);
		if (conn instanceof HttpURLConnection) {
			((HttpURLConnection) conn).setRequestMethod("PUT");
		} else {
			throw new IllegalStateException("Unknown connection type");
		}

		conn.setRequestProperty("Content-type", "application/octet-stream");
		conn.setRequestProperty("Content-length", "" + fileSize);
		return conn;
	}

	private String getFileUploadUrl(String remotePathName, long fileSize,
			VsphereConnHandler connHandler, ManagedObjectReference fileManager)
			throws
            RuntimeFaultFaultMsg, FileFaultFaultMsg,
			GuestOperationsFaultFaultMsg, InvalidStateFaultMsg,
			TaskInProgressFaultMsg {

		GuestFileAttributes gfa = new GuestFileAttributes();
		final boolean override = true;
		String fileUploadUrl = connHandler
				.getClient()
				.getVimPort()
				.initiateFileTransferToGuest(fileManager, vm, credentials,
						remotePathName, gfa, fileSize, override);

		// replace * with the address of the server. see vsphere docs.
		fileUploadUrl = fileUploadUrl.replace("*", connHandler.getClient().getServer());
		return fileUploadUrl;
	}

	@Override
	public OutputStream getOutputStream() throws IOException {
		VsphereConnHandler connHandler = null;
		try {
			connHandler = getConnHandler();
			ManagedObjectReference fileManager = getFileManager(connHandler);

			VSphereOutputStream c = new VSphereOutputStream(connHandler,
					fileManager, vm, credentials, getPathInVm());
			// passed owner ship
			connHandler = null;
			return c;
		} catch (InvalidPropertyFaultMsg | RuntimeFaultFaultMsg e) {
			translateandLogException(e);
		} finally {
			releaseConnHandler(connHandler);
		}
		throw new RuntimeException("Should never get here");
	}

	@Override
	@UnsupportedFileOperation
	public OutputStream getAppendOutputStream() throws IOException {
		throw new UnsupportedFileOperationException(FileOperation.APPEND_FILE);
	}

	@Override
	@UnsupportedFileOperation
	public RandomAccessInputStream getRandomAccessInputStream()
			throws IOException {
		throw new UnsupportedFileOperationException(
				FileOperation.RANDOM_READ_FILE);
	}

	@Override
	@UnsupportedFileOperation
	public RandomAccessOutputStream getRandomAccessOutputStream()
			throws IOException {
		throw new UnsupportedFileOperationException(FileOperation.RANDOM_WRITE_FILE);
	}

	@Override
	public void delete() throws IOException {
		VsphereConnHandler connHandler = null;
		try {
			connHandler = getConnHandler();
			ManagedObjectReference fileManager = getFileManager(connHandler);
			if (isDirectory()) {
				connHandler
						.getClient()
						.getVimPort()
						.deleteDirectoryInGuest(fileManager, vm, credentials,
								getPathInVm(), false);
				isDir = false;

			} else {
				connHandler
						.getClient()
						.getVimPort()
						.deleteFileInGuest(fileManager, vm, credentials,
								getPathInVm());
				isFile = isSymLink = false;
			}
		} catch (FileFaultFaultMsg | GuestOperationsFaultFaultMsg | InvalidStateFaultMsg | RuntimeFaultFaultMsg |
                TaskInProgressFaultMsg | InvalidPropertyFaultMsg e) {
			translateandLogException(e);
		} finally {
			releaseConnHandler(connHandler);
		}

	}

	@Override
	public void renameTo(AbstractFile destFile) throws IOException {

		// can't copy\rename to a different host
		// os might be windows, so can't rename with different case.
		checkRenamePrerequisites(destFile, false, false);

		VsphereConnHandler connHandler = null;
		try {
			connHandler = getConnHandler();
			ManagedObjectReference fileManager = getFileManager(connHandler);
			if (isDirectory()) {
				connHandler
						.getClient()
						.getVimPort()
						.moveDirectoryInGuest(fileManager, vm, credentials,
								getPathInVm(),
								((VSphereFile) destFile).getPathInVm());

			} else {
				connHandler
						.getClient()
						.getVimPort()
						.moveFileInGuest(fileManager, vm, credentials,
								getPathInVm(),
								((VSphereFile) destFile).getPathInVm(), true);
			}
		} catch (FileFaultFaultMsg | GuestOperationsFaultFaultMsg | RuntimeFaultFaultMsg | InvalidStateFaultMsg | InvalidPropertyFaultMsg | TaskInProgressFaultMsg e) {
			translateandLogException(e);
		} finally {
			releaseConnHandler(connHandler);
		}

	}

	private static void translateandLogException(Exception e)
			throws IOException {
		LOGGER.error("Error with vsphere remote ops", e);
		throw new IOException(e);
	}

	@Override
	@UnsupportedFileOperation
	public void copyRemotelyTo(AbstractFile destFile) throws IOException {
		throw new UnsupportedFileOperationException(FileOperation.COPY_REMOTELY);
	}

	@Override
	@UnsupportedFileOperation
	public long getFreeSpace() throws IOException {
		throw new UnsupportedFileOperationException(
				FileOperation.GET_FREE_SPACE);
	}

	@Override
	@UnsupportedFileOperation
	public long getTotalSpace() throws IOException {

		throw new UnsupportedFileOperationException(
				FileOperation.GET_TOTAL_SPACE);
	}

	@Override
	@UnsupportedFileOperation
	public short getReplication() throws UnsupportedFileOperationException {
		throw new UnsupportedFileOperationException(FileOperation.GET_REPLICATION);
	}

	@Override
	@UnsupportedFileOperation
	public long getBlocksize() throws UnsupportedFileOperationException {
		throw new UnsupportedFileOperationException(FileOperation.GET_BLOCKSIZE);
	}

	@Override
	@UnsupportedFileOperation
	public void changeReplication(short replication) throws IOException {
		throw new UnsupportedFileOperationException(FileOperation.CHANGE_REPLICATION);
	}

	@Override
	public Object getUnderlyingFileObject() {
		return null;
	}

	@Override
	public AbstractFile getRoot() {
		FileURL rootURL = (FileURL) getURL().clone();
		String rootPath = getRootPath();
		rootURL.setPath("/" + vmIdentifier + "/" + rootPath);

		return FileFactory.getFile(rootURL);
	}

	private String getRootPath() {
		if (isWinPath()) {
			return getPathInVm().substring(0, 2);
		}

		return "";
	}

	private boolean isWinPath() {
		return (pathInsideVm.length() >= 2) && (pathInsideVm.charAt(1) == ':');
	}

}
