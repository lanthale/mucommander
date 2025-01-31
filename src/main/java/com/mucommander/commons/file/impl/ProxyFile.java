package com.mucommander.commons.file.impl;

import com.mucommander.commons.file.*;
import com.mucommander.commons.file.filter.FileFilter;
import com.mucommander.commons.file.filter.FilenameFilter;
import com.mucommander.commons.io.FileTransferException;
import com.mucommander.commons.io.RandomAccessInputStream;
import com.mucommander.commons.io.RandomAccessOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * ProxyFile is an {@link AbstractFile} that acts as a proxy between the class that extends it
 * and the proxied <code>AbstractFile</code> instance specified to the constructor.
 * All <code>AbstractFile</code> public methods (abstract or not) are delegated to the proxied file.
 * The {@link #getProxiedFile()} method allows to retrieve the proxied file instance.
 *
 * <p>This class is useful for wrapper files, such as {@link com.mucommander.commons.file.AbstractArchiveFile archive files},
 * that provide additional functionality over an existing <code>AbstractFile</code> instance (the proxied file).
 * By implementing/overriding every <code>AbstractFile</code> methods, <code>ProxyFile</code> ensures that
 * all <code>AbstractFile</code> methods can safely be used, even if they are overridden by the proxied
 * file instance's class.
 *
 * <p><b>Implementation note:</b> the <code>java.lang.reflect.Proxy</code> class can unfortunately not be
 * used as it only works with interfaces (not abstract class). There doesn't seem to be any dynamic way to
 * proxy method invocations, so any modifications made to {@link com.mucommander.commons.file.AbstractFile} must be also
 * reflected in <code>ProxyFile</code>.
 *
 * @see com.mucommander.commons.file.AbstractArchiveFile
 * @author Maxence Bernard
 */
public abstract class ProxyFile extends AbstractFile {
    private static Logger logger;

    /** The proxied file instance */
    protected AbstractFile file;


    /**
     * Creates a new ProxyFile using the given file to delegate AbstractFile method calls to.
     *
     * @param file the file to be proxied
     */
    public ProxyFile(AbstractFile file) {
        super(file.getURL());
        this.file = file;
    }

    /**
     * Returns the <code>AbstractFile</code> instance proxied by this <code>ProxyFile</code>.
     *
     * @return the <code>AbstractFile</code> instance proxied by this <code>ProxyFile</code>
     */
    public AbstractFile getProxiedFile() {
        return file;
    }


    /////////////////////////////////
    // AbstractFile implementation //
    /////////////////////////////////

    @Override
    public long getLastModifiedDate() {
        return file.getLastModifiedDate();
    }

    @Override
    public void setLastModifiedDate(long lastModified) throws IOException {
        file.setLastModifiedDate(lastModified);
    }

    @Override
    public long getSize() {
        return file.getSize();
    }

    @Override
    public AbstractFile getParent() {
        return file.getParent();
    }

    @Override
    public void setParent(AbstractFile parent) {
        file.setParent(parent);
    }

    @Override
    public boolean exists() {
        return file.exists();
    }

    @Override
    public void changePermission(int access, int permission, boolean enabled) throws IOException {
        file.changePermission(access, permission, enabled);
    }

    @Override
    public String getOwner() {
        return file.getOwner();
    }

    @Override
    public boolean canGetOwner() {
        return file.canGetOwner();
    }

    @Override
    public String getGroup() {
        return file.getGroup();
    }

    @Override
    public boolean canGetGroup() {
        return file.canGetGroup();
    }

    @Override
    public boolean isDirectory() {
        return file.isDirectory();
    }

    @Override
    public boolean isSymlink() {
        return file.isSymlink();
    }

    @Override
    public boolean isSystem() {
        return file.isSystem();
    }

    @Override
    public AbstractFile[] ls() throws IOException {
        return file.ls();
    }

    @Override
    public void mkdir() throws IOException {
        file.mkdir();
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return file.getInputStream();
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        return file.getOutputStream();
    }

    @Override
    public OutputStream getAppendOutputStream() throws IOException {
        return file.getAppendOutputStream();
    }

    @Override
    public RandomAccessInputStream getRandomAccessInputStream() throws IOException {
        return file.getRandomAccessInputStream();
    }

    @Override
    public RandomAccessOutputStream getRandomAccessOutputStream() throws IOException {
        return file.getRandomAccessOutputStream();
    }

    @Override
    public void delete() throws IOException {
        file.delete();
    }

    @Override
    public void copyRemotelyTo(AbstractFile destFile) throws IOException {
        file.copyRemotelyTo(destFile);
    }

    @Override
    public void renameTo(AbstractFile destFile) throws IOException {
        file.renameTo(destFile);
    }

    @Override
    public long getFreeSpace() throws IOException {
        return file.getFreeSpace();
    }

    @Override
    public long getTotalSpace() throws IOException {
        return file.getTotalSpace();
    }

    @Override
    public Object getUnderlyingFileObject() {
        return file.getUnderlyingFileObject();
    }

    
    /////////////////////////////////////
    // Overridden AbstractFile methods //
    /////////////////////////////////////

    @Override
    public boolean isFileOperationSupported(FileOperation op) {
        Class<? extends AbstractFile> thisClass = getClass();
        Method opMethod = op.getCorrespondingMethod(thisClass);
        // If the method corresponding to the file operation has been overridden by this class (a ProxyFile subclass),
        // check the presence of the UnsupportedFileOperation annotation in this class.
        try {
            if (!thisClass.getMethod(opMethod.getName(), opMethod.getParameterTypes()).getDeclaringClass().equals(ProxyFile.class)) {
                return AbstractFile.isFileOperationSupported(op, thisClass);
            }
        } catch(Exception e) {
            // Should never happen, unless AbstractFile method signatures have changed and FileOperation has not been updated
            getLogger().warn("Exception caught, this should not have happened", e);
        }

        // Otherwise, check for the presence of the UnsupportedFileOperation annotation in the wrapped AbstractFile.
        return file.isFileOperationSupported(op);
    }

    @Override
    public FileURL getURL() {
        return file.getURL();
    }

    @Override
    public URL getJavaNetURL() throws MalformedURLException {
        return file.getJavaNetURL();
    }

    @Override
    public String getName() {
        return file.getName();
    }

    @Override
    public String getExtension() {
        return file.getExtension();
    }

    @Override
    public String getBaseName() {
    	return file.getBaseName();
    }
    
    @Override
    public String getAbsolutePath() {
        return file.getAbsolutePath();
    }

    @Override
    public String getCanonicalPath() {
        return file.getCanonicalPath();
    }

    @Override
    public AbstractFile getCanonicalFile() {
        return file.getCanonicalFile();
    }

    @Override
    public String getSeparator() {
        return file.getSeparator();
    }

    @Override
    public boolean isArchive() {
        return file.isArchive();
    }

    @Override
    public boolean isHidden() {
        return file.isHidden();
    }

    @Override
    public FilePermissions getPermissions() {
        return file.getPermissions();
    }

    @Override
    public void changePermissions(int permissions) throws IOException {
        file.changePermissions(permissions);
    }

    @Override
    public PermissionBits getChangeablePermissions() {
        return file.getChangeablePermissions();
    }

    @Override
    public String getPermissionsString() {
        return file.getPermissionsString();
    }

    @Override
    public AbstractFile getRoot() {
        return file.getRoot();
    }

    @Override
    public boolean isRoot() {
        return file.isRoot();
    }

    @Override
    public AbstractFile getVolume() {
        return file.getVolume();
    }

    @Override
    public InputStream getInputStream(long offset) throws IOException {
        return file.getInputStream(offset);
    }

    @Override
    public void copyStream(InputStream in, boolean append, long length) throws FileTransferException {
        file.copyStream(in, append, length);
    }

    @Override
    public AbstractFile[] ls(FileFilter filter) throws IOException {
        return file.ls(filter);
    }

    @Override
    public AbstractFile[] ls(FilenameFilter filter) throws IOException {
        return file.ls(filter);
    }

    @Override
    public void mkfile() throws IOException {
        file.mkfile();
    }

    @Override
    public void deleteRecursively() throws IOException {
        file.deleteRecursively();
    }

    @Override
    public boolean equals(Object f) {
        return file.equals(f);
    }

    @Override
    public boolean equalsCanonical(Object f) {
        return file.equalsCanonical(f);
    }

    public int hashCode() {
        return file.hashCode();
    }

    public String toString() {
        return file.toString();
    }

    @Override
    public short getReplication() throws UnsupportedFileOperationException {
        return file.getReplication();
    }

    @Override
    public long getBlocksize() throws UnsupportedFileOperationException {
        return file.getBlocksize();
    }

    @Override
    public void changeReplication(short replication) throws IOException {
        file.changeReplication(replication);
    }

    @Override
    public boolean isExecutable() {
    	return file.isExecutable();
    }

    @Override
    public PushbackInputStream getPushBackInputStream(int bufferSize) throws IOException {
    	return file.getPushBackInputStream(bufferSize);
    }

    @Override
    public void closePushbackInputStream() throws IOException {
    	file.closePushbackInputStream();
    }

    private static Logger getLogger() {
        if (logger == null) {
            logger = LoggerFactory.getLogger(ProxyFile.class);
        }
        return logger;
    }
}
