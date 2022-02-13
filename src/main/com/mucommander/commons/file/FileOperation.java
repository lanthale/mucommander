package com.mucommander.commons.file;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

/**
 * @author Maxence Bernard
 * @see UnsupportedFileOperationException
 * @see AbstractFile
 */
public enum FileOperation {
    /**
     * Represents a 'read' operation, as specified by {@link AbstractFile#getInputStream()}.
     *
     * @see AbstractFile#getInputStream()
     **/
    READ_FILE,

    /**
     * Represents a 'random read' operation, as specified by {@link AbstractFile#getRandomAccessInputStream()}.
     *
     * @see AbstractFile#getRandomAccessInputStream()
     **/
    RANDOM_READ_FILE,

    /**
     * Represents a 'write' operation, as specified by {@link AbstractFile#getOutputStream()}.
     *
     * @see AbstractFile#getOutputStream()
     **/
    WRITE_FILE,

    /**
     * Represents an 'append' operation, as specified by {@link AbstractFile#getAppendOutputStream()}.
     *
     * @see AbstractFile#getAppendOutputStream()
     **/
    APPEND_FILE,

    /**
     * Represents a 'random write' operation, as specified by {@link AbstractFile#getRandomAccessOutputStream()}.
     *
     * @see AbstractFile#getRandomAccessOutputStream()
     **/
    RANDOM_WRITE_FILE,

    /**
     * Represents an 'mkdir' operation, as specified by {@link AbstractFile#mkdir()}.
     *
     * @see AbstractFile#mkdir()
     **/
    CREATE_DIRECTORY,

    /**
     * Represents an 'ls' operation, as specified by {@link AbstractFile#ls()}.
     *
     * @see AbstractFile#ls()
     **/
    LIST_CHILDREN,

    /**
     * Represents a 'delete' operation, as specified by {@link AbstractFile#delete()}.
     *
     * @see AbstractFile#delete()
     **/
    DELETE,

    /**
     * Represents a 'remove copy' operation, as specified by {@link AbstractFile#copyRemotelyTo(AbstractFile)}.
     */
    COPY_REMOTELY,

    /**
     * Represents a 'rename' operation, as specified by {@link AbstractFile#renameTo(AbstractFile)}.
     */
    RENAME,

    /**
     * Represents a 'change date' operation, as specified by {@link AbstractFile#setLastModifiedDate(long)}.
     *
     * @see AbstractFile#setLastModifiedDate(long)
     **/
    CHANGE_DATE,

    /**
     * Represents a 'change permission' operation, as specified by {@link AbstractFile#changePermission(int, int, boolean)}.
     */
    CHANGE_PERMISSION,

    /**
     * Represents a 'get free space' operation, as specified by {@link AbstractFile#getFreeSpace()}.
     */
    GET_FREE_SPACE,

    /**
     * Represents a 'change replication factor' operation, as specified by {@link AbstractFile#changeReplication(short)}}.
     */
    GET_BLOCKSIZE,

    /**
     * Represents a 'change replication factor' operation, as specified by {@link AbstractFile#changeReplication(short)}}.
     */
    GET_REPLICATION,

    /**
     * Represents a 'change replication factor' operation, as specified by {@link AbstractFile#changeReplication(short)}}.
     */
    CHANGE_REPLICATION,

    /**
     * Represents a 'get total space' operation, as specified by {@link AbstractFile#getTotalSpace()}.
     */
    GET_TOTAL_SPACE;

    private static final Logger LOGGER = LoggerFactory.getLogger(FileOperation.class);

    /**
     * Returns the {@link AbstractFile} method corresponding to this file operation.
     *
     * @param c the AbstractFile class for which to return a <code>Method</code> object.
     * @return the {@link AbstractFile} method corresponding to this file operation.
     */
    public Method getCorrespondingMethod(Class<? extends AbstractFile> c) {
        try {
            switch(this) {
                case READ_FILE:
                    return c.getMethod("getInputStream");

                case RANDOM_READ_FILE:
                    return c.getMethod("getRandomAccessInputStream");

                case WRITE_FILE:
                    return c.getMethod("getOutputStream");

                case APPEND_FILE:
                    return c.getMethod("getAppendOutputStream");

                case RANDOM_WRITE_FILE:
                    return c.getMethod("getRandomAccessOutputStream");

                case CREATE_DIRECTORY:
                    return c.getMethod("mkdir");

                case LIST_CHILDREN:
                    return c.getMethod("ls");

                case CHANGE_DATE:
                    return c.getMethod("setLastModifiedDate", Long.TYPE);

                case GET_BLOCKSIZE:
                    return c.getMethod("getBlocksize");

                case GET_REPLICATION:
                    return c.getMethod("getReplication");

                case CHANGE_REPLICATION:
                    return c.getMethod("changeReplication", Short.TYPE);

                case CHANGE_PERMISSION:
                    return c.getMethod("changePermission", Integer.TYPE, Integer.TYPE, Boolean.TYPE);

                case DELETE:
                    return c.getMethod("delete");

                case RENAME:
                    return c.getMethod("renameTo", AbstractFile.class);

                case COPY_REMOTELY:
                    return c.getMethod("copyRemotelyTo", AbstractFile.class);

                case GET_FREE_SPACE:
                    return c.getMethod("getFreeSpace");

                case GET_TOTAL_SPACE:
                    return c.getMethod("getTotalSpace");

                default:
                    // This should never be reached, unless method signatures have changed and this method hasn't been updated.
                    LOGGER.warn("this line should not have been executed");
                    return null;
            }
        } catch (Exception e) {
            // Should never happen, unless method signatures have changed and this method hasn't been updated.
            LOGGER.warn("this line should not have been executed", e);
            return null;
        }
    }
}
