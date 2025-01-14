/*
 * This file is part of muCommander, http://www.mucommander.com
 * Copyright (C) 2002-2010 Maxence Bernard
 *
 * muCommander is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * muCommander is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */


package com.mucommander.commons.file.util;

import com.dd.plist.BinaryPropertyListParser;
import com.dd.plist.NSString;
//import com.dd.plist.PropertyListFormatException;
import com.mucommander.commons.file.AbstractFile;
import com.mucommander.commons.io.StreamUtils;
import com.mucommander.commons.runtime.OsFamily;
import com.mucommander.commons.runtime.OsVersion;
import com.sun.jna.platform.mac.XAttrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class contains methods for file operations that are specific to Mac OS X.
 *
 * @author Maxence Bernard
 */
public class OSXFileUtils {

    /** AppleScript that sets file comment */
    public static final String SET_COMMENT_APPLESCRIPT = "tell application \"Finder\" to set comment of file {posix file \"%s\"} to \"%s\"";

    private static final Logger LOGGER = LoggerFactory.getLogger(OSXFileUtils.class);

    /**
     * Returns the Spotlight/Finder comment of the given file. The specified file must be a LocalFile,
     * or have a LocalFile as an ancestor.
     *
     * <p>
     *  <code>null</code> is returned in any of the following cases:
     *  <ul>
     *   <li>if the current OS is not Mac OS X or if the version is not 10.4 or higher (i.e. Spotlight is not available)</li>
     *   <li>if the specified file is not a LocalFile and does not have a LocalFile ancestor</li>
     *   <li>if the specified file has no comment</li>
     *   <li>if the comment could not be retrieved (for any reason)</li>
     *  </ul>
     *
     * @param file a local file
     * @return the Spotlight/Finder comment of the specified file
     */
    public static String getSpotlightComment(AbstractFile file) {
        if (!OsVersion.MAC_OS_X_10_4.isCurrentOrHigher()) {
            return null;
        }
        byte[] bytes = XAttrUtils.read(file.getAbsolutePath(), XAttrUtils.COMMENT);
        if (bytes == null)
            return null;

        try {
            NSString comment = (NSString) BinaryPropertyListParser.parse(bytes);
            return comment.getContent();
        } catch (UnsupportedEncodingException e) {
            LOGGER.error("failed to read comment of: " + file.getAbsolutePath());
            return null;
        } catch (Exception ex) {
            LOGGER.error("failed to read comment of: " + file.getAbsolutePath());
        }
        return null;

    }
}
