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

package com.mucommander.commons.runtime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class represents a non-versioned family of operating system, like <code>Windows</code> or <code>Linux</code>. 
 * The current runtime instance is determined using the value of the <code>os.name</code> system property.
 *
 * @see OsVersion
 * @author Maxence Bernard, Arik Hadas
 */
public enum OsFamily {
	/** Windows */
    WINDOWS("Windows",false),
    /** Mac OS X */
    MAC_OS_X("Mac OS X"),
    /** Linux */
    LINUX("Linux"),
    /** Solaris */
    SOLARIS("Solaris"),
    /** OS/2 */
    OS_2("OS/2"),
    /** FreeBSD */
    FREEBSD("FreeBSD"),
    /** AIX */
    AIX("AIX"),
    /** HP-UX */
    HP_UX("HP-UX"),
    /** OpenVMS */
    OPENVMS("OpenVMS"),
    /** Other OS */
    UNKNOWN_OS_FAMILY("Unknown");

    /** Logger used by this class. */
    private static Logger logger;

    /** The String representation of this RuntimeProperty, set at creation time */
    private final String stringRepresentation;
    
    private final boolean isCaseSensitiveFilesystem;

    /** Holds the OsFamily of the current runtime environment  */
    private static OsFamily currentValue;

    
    OsFamily(String stringRepresentation) {
    	this(stringRepresentation,true);
    }
    
    OsFamily(String stringRepresentation, boolean isCaseSensitiveFilesystem) {
    	this.stringRepresentation = stringRepresentation;
    	this.isCaseSensitiveFilesystem = isCaseSensitiveFilesystem;
    }


    /**
     * Returns the OS family of the current runtime environment.
     *
     * @return the OS family of the current runtime environment
     */
    public static OsFamily getCurrent() {
        if (currentValue == null) {
            currentValue = parseSystemProperty(getRawSystemProperty());
        }
        return currentValue;
    }

    /**
     * Returns <code>true</code> if this OS family is UNIX-based. The following OS families are considered UNIX-based:
     * <ul>
     *  <li>{@link #LINUX}</li>
     *  <li>{@link #MAC_OS_X}</li>
     *  <li>{@link #SOLARIS}</li>
     *  <li>{@link #FREEBSD}</li>
     *  <li>{@link #AIX}</li>
     *  <li>{@link #HP_UX}</li>
     *  <li>{@link #UNKNOWN_OS_FAMILY}: the reason for this being that most alternative OSes are Unix-based.</li>
     * </ul>
     *
     * @return <code>true</code> if the current OS is UNIX-based
     */
    public boolean isUnixBased() {
        return this == MAC_OS_X
                || this == LINUX
                || this == SOLARIS
                || this == FREEBSD
                || this == AIX
                || this == HP_UX
                || this == UNKNOWN_OS_FAMILY;

        // Not UNIX-based: WINDOWS, OS/2 and OpenVMS
    }

    /**
     * Returns the value of the system property which serves to detect the OS family at runtime.
     *
     * @return the value of the system property which serves to detect the OS family at runtime.
     */
    public static String getRawSystemProperty() {
        return System.getProperty("os.name");
    }

    /**
     * Returns an <code>OsFamily</code> instance corresponding to the specified system property's value.
     *
     * @param osNameProp the value of the "os.name" system property
     * @return an OsFamily instance corresponding to the specified system property's value
     */
    static OsFamily parseSystemProperty(String osNameProp) {
        // This website holds a collection of system property values under many OSes:
        // http://lopica.sourceforge.net/os.html

        // Windows family
        if (osNameProp.startsWith("Windows")) {
            return WINDOWS;
        }
        // Mac OS X family
        if (osNameProp.startsWith("Mac OS X")) {
            return MAC_OS_X;
        }
        // OS/2 family
        if (osNameProp.startsWith("OS/2")) {
            return OS_2;
        }
        // Linux family
        if (osNameProp.startsWith("Linux")) {
            return LINUX;
        }
        // Solaris family
        if (osNameProp.startsWith("Solaris") || osNameProp.startsWith("SunOS")) {
            return SOLARIS;
        }
        if (osNameProp.startsWith("FreeBSD")) {
            return FREEBSD;
        }
        if (osNameProp.startsWith("AIX")) {
            return AIX;
        }
        if (osNameProp.startsWith("HP-UX")) {
            return HP_UX;
        }
        if (osNameProp.startsWith("OpenVMS")) {
            return OPENVMS;
        }

        // Any other OS
        return UNKNOWN_OS_FAMILY;
    }

    /**
     * Returns <code>true</code> if this instance is the same instance as the one returned by {@link #getCurrent()}.
     *
     * @return true if this instance is the same as the current runtime's value
     */
    public boolean isCurrent() {
        return this == currentValue;
    }


    @Override
    public String toString() {
        return stringRepresentation;
    }


	public boolean isCaseSensitiveFilesystem() {
		return isCaseSensitiveFilesystem;
	}

    private static Logger getLogger() {
        if (logger == null) {
            logger = LoggerFactory.getLogger(OsFamily.class);
        }
        return logger;
    }
}
