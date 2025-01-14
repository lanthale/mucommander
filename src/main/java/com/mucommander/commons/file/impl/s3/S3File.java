package com.mucommander.commons.file.impl.s3;

import com.mucommander.commons.file.*;
import com.mucommander.commons.io.RandomAccessOutputStream;
import com.mucommander.commons.runtime.JavaVersion;
import org.jets3t.service.Constants;
import org.jets3t.service.S3ObjectsChunk;
import org.jets3t.service.S3Service;
import org.jets3t.service.S3ServiceException;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jets3t.service.ServiceException;
import org.jets3t.service.StorageObjectsChunk;
import org.jets3t.service.model.StorageObject;

/**
 * Super class of {@link S3Root}, {@link S3Bucket} and {@link S3Object}.
 *
 * @author Maxence Bernard
 */
public abstract class S3File extends ProtocolFile {

    protected org.jets3t.service.S3Service service;

    protected AbstractFile parent;
    protected boolean parentSet;

    protected S3File(FileURL url, S3Service service) {
        super(url);

        this.service = service;
    }
    
    protected IOException getIOException(S3ServiceException e) throws IOException {
        return getIOException(e, fileURL);
    }

    protected static IOException getIOException(S3ServiceException e, FileURL fileURL) throws IOException {
        handleAuthException(e, fileURL);

        Throwable cause = e.getCause();
        if(cause instanceof IOException)
            return (IOException)cause;

        if(JavaVersion.JAVA_1_6.isCurrentOrHigher())
            return new IOException(e);

        return new IOException(e.getMessage());
    }

    protected static void handleAuthException(S3ServiceException e, FileURL fileURL) throws AuthException {
        int code = e.getResponseCode();
        if(code==401 || code==403)
            throw new AuthException(fileURL);
    }
    
    protected AbstractFile[] listObjects(String bucketName, String prefix, S3File parent) throws IOException {
        try {
            StorageObjectsChunk chunk = service.listObjectsChunked(bucketName, prefix, "/", Constants.DEFAULT_OBJECT_LIST_CHUNK_SIZE, null, true);
            StorageObject[] objects = chunk.getObjects();
            String[] commonPrefixes = chunk.getCommonPrefixes();

            if (objects.length == 0 && !prefix.isEmpty()) {
                // This happens only when the directory does not exist
                throw new IOException();
            }

            AbstractFile[] children = new AbstractFile[objects.length+commonPrefixes.length];
            FileURL childURL;
            int i = 0;
            String objectKey;

            for(StorageObject object : objects) {
                // Discard the object corresponding to the prefix itself
                objectKey = object.getKey();
                if(objectKey.equals(prefix))
                    continue;

                childURL = (FileURL)fileURL.clone();
                childURL.setPath(bucketName + "/" + objectKey);

                children[i] = FileFactory.getFile(childURL, parent, service, object);
                i++;
            }

            org.jets3t.service.model.S3Object directoryObject;
            for(String commonPrefix : commonPrefixes) {
                childURL = (FileURL)fileURL.clone();
                childURL.setPath(bucketName + "/" + commonPrefix);

                directoryObject = new org.jets3t.service.model.S3Object(commonPrefix);
                // Common prefixes are not objects per se, and therefore do not have a date, content-length nor owner.
                directoryObject.setLastModifiedDate(new Date(System.currentTimeMillis()));
                directoryObject.setContentLength(0);
                children[i] = FileFactory.getFile(childURL, parent, service, directoryObject);
                i++;
            }

            // Trim the array if an object was discarded.
            // Note: Having to recreate an array sucks (puts pressure on the GC), but I haven't found a reliable way
            // to know in advance whether the prefix will appear in the results or not.
            if(i<children.length) {
                AbstractFile[] childrenTrimmed = new AbstractFile[i];
                System.arraycopy(children, 0, childrenTrimmed, 0, i);

                return childrenTrimmed;
            }

            return children;
        }
        catch(S3ServiceException e) {
            throw getIOException(e);
        } catch (ServiceException ex) {
            Logger.getLogger(S3File.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }


    //////////////////////
    // Abstract methods //
    //////////////////////

    public abstract FileAttributes getFileAttributes();


    /////////////////////////////////
    // ProtocolFile implementation //
    /////////////////////////////////

    @Override
    public AbstractFile getParent() {
        if(!parentSet) {
            FileURL parentFileURL = this.fileURL.getParent();
            if(parentFileURL!=null) {
                try {
                    parent = FileFactory.getFile(parentFileURL, null, service);
                }
                catch(IOException e) {
                    // No parent
                }
            }

            parentSet = true;
        }

        return parent;
    }

    @Override
    public void setParent(AbstractFile parent) {
        this.parent = parent;
        this.parentSet = true;
    }


    // Delegates to FileAttributes

    @Override
    public long getLastModifiedDate() {
        return getFileAttributes().getLastModifiedDate();
    }

    @Override
    public long getSize() {
        return getFileAttributes().getSize();
    }

    @Override
    public boolean exists() {
        return getFileAttributes().exists();
    }

    @Override
    public boolean isDirectory() {
        return getFileAttributes().isDirectory();
    }

    @Override
    public FilePermissions getPermissions() {
        return getFileAttributes().getPermissions();
    }

    @Override
    public Object getUnderlyingFileObject() {
        return getFileAttributes();
    }
    

    // Unsupported operations, no matter the kind of resource (object, bucket, service)

    @Override
    public boolean isSymlink() {
        return false;
    }

    @Override
    public boolean isSystem() {
        return false;
    }

    @Override
    public PermissionBits getChangeablePermissions() {
        return PermissionBits.EMPTY_PERMISSION_BITS;
    }

    @Override
    @UnsupportedFileOperation
    public void changePermission(int access, int permission, boolean enabled) throws UnsupportedFileOperationException {
        throw new UnsupportedFileOperationException(FileOperation.CHANGE_PERMISSION);
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
    @UnsupportedFileOperation
    public OutputStream getAppendOutputStream() throws UnsupportedFileOperationException {
        throw new UnsupportedFileOperationException(FileOperation.APPEND_FILE);
    }

    @Override
    @UnsupportedFileOperation
    public RandomAccessOutputStream getRandomAccessOutputStream() throws UnsupportedFileOperationException {
        throw new UnsupportedFileOperationException(FileOperation.RANDOM_WRITE_FILE);
    }

    @Override
    @UnsupportedFileOperation
    public long getFreeSpace() throws UnsupportedFileOperationException {
        throw new UnsupportedFileOperationException(FileOperation.GET_FREE_SPACE);
    }

    @Override
    @UnsupportedFileOperation
    public long getTotalSpace() throws UnsupportedFileOperationException {
        throw new UnsupportedFileOperationException(FileOperation.GET_TOTAL_SPACE);
    }

    @Override
    @UnsupportedFileOperation
    public void setLastModifiedDate(long lastModified) throws UnsupportedFileOperationException {
        throw new UnsupportedFileOperationException(FileOperation.CHANGE_DATE);
    }
}
