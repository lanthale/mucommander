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
 * This class represents a major version of an operating system, like <code>Mac OS X 10.5</code> or
 * <code>Windows XP</code>. The current runtime value is determined using the value of the <code>os.version</code>
 * system property and the current {@link OsFamily} instance.
 * Being a {@link com.mucommander.commons.runtime.ComparableRuntimeProperty}, OS versions are ordered and can be compared
 * against each other.
 *
 * @see OsFamily
 * @author Maxence Bernard, Arik Hadas
 */
public enum OsVersion implements ComparableRuntimeProperty {
	/** Unknown OS version */
	UNKNOWN_VERSION("Unknown"),

	//////////////////////
	// Windows versions //
	//////////////////////

	// Windows 9X subfamily

	/** Windows 95 */
	WINDOWS_95("Windows 95"),
	/** Windows 98 */
	WINDOWS_98("Windows 98"),
	/** Windows Me */
	WINDOWS_ME("Windows Me"),

	// Windows NT subfamily

	/** Windows NT */
	WINDOWS_NT("Windows NT"),
	/** Windows 2000 */
	WINDOWS_2000("Windows 2000"),
	/** Windows XP */
	WINDOWS_XP("Windows XP"),
	/** Windows 2003 */
	WINDOWS_2003("Windows 2003"),
	/** Windows Vista */
	WINDOWS_VISTA("Windows Vista"),
	/** Windows 7 */
	WINDOWS_7("Windows 7"),
	/** Windows 8 */
	WINDOWS_8("Windows 8"),
    /** Windows 8 */
    WINDOWS_8_1("Windows 8.1"),
    /** Windows 10 */
    WINDOWS_10("Windows 10"),


	///////////////////////
	// Mac OS X versions //
	///////////////////////

	/** Mac OS X 10.0 (Cheetah) */
	MAC_OS_X_10_0("10.0"),

	/** Mac OS X 10.1 (Puma) */
	MAC_OS_X_10_1("10.1"),

	/** Mac OS X 10.2 (Jaguar) */
	MAC_OS_X_10_2("10.2"),

	/** Mac OS X 10.3 (Panther) */
	MAC_OS_X_10_3("10.3"),

	/** Mac OS X 10.4 (Tiger) */
	MAC_OS_X_10_4("10.4"),

	/** Mac OS X 10.5 (Leopard) */
	MAC_OS_X_10_5("10.5"),

	/** Mac OS X 10.6 (Snow Leopard) */
	MAC_OS_X_10_6("10.6"),

	/** Mac OS X 10.7 (Lion) */
	MAC_OS_X_10_7("10.7"),

	/** Mac OS X 10.8 (Mountain Lion) */
	MAC_OS_X_10_8("10.8"),

    /** Mac OS X 10.9 (Mavericks) */
    MAC_OS_X_10_9("10.9"),

    /** Mac OS X 10.10 (Yosemite) */
    MAC_OS_X_10_10("10.10"),

    /** Mac OS X 10.11 (El Capitan) */
    MAC_OS_X_10_11("10.11"),

    /** Mac OS X 10.12 (Sierra) */
    MAC_OS_X_10_12("10.12"),

    /** Mac OS X 10.13 (High Sierra) */
    MAC_OS_X_10_13("10.13"),

    /** Mac OS X 10.14 (Mojave) */
    MAC_OS_X_10_14("10.14"),

    /** Mac OS X 10.15 (Catalina) */
    MAC_OS_X_10_15("10.15"),

    /** Mac OS X 10.16 (Big Sur) */
    MAC_OS_X_10_16("10.16");



    /** Logger used by this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(OsVersion.class);

    /** The String representation of this RuntimeProperty, set at creation time */
    private final String stringRepresentation;

    /** Holds the OsVersion of the current runtime environment  */
    private static final OsVersion currentValue;

    static {
    	currentValue = parseSystemProperty(getRawSystemProperty(), OsFamily.getRawSystemProperty(), OsFamily.getCurrent());
    	LOGGER.info("Current OS version: {}", currentValue);
    }


    OsVersion(String stringRepresentation) {
    	this.stringRepresentation = stringRepresentation;
    }


    /**
     * Returns the OS version of the current runtime environment.
     *
     * @return the OS version of the current runtime environment
     */
    public static OsVersion getCurrent() {
        return currentValue;
    }

    /**
     * Returns the value of the system property which serves to detect the OS version at runtime.
     *
     * @return the value of the system property which serves to detect the OS version at runtime.
     */
    public static String getRawSystemProperty() {
        return System.getProperty("os.version");
    }

    /**
     * Returns an <code>OsVersion</code> instance corresponding to the specified system property's value.
     *
     * @param osVersionProp the value of the "os.version" system property
     * @param osNameProp the value of the "os.name" system property
     * @param osFamily the current OS family
     * @return an OsVersion instance corresponding to the specified system property's value
     */
    static OsVersion parseSystemProperty(String osVersionProp, String osNameProp, OsFamily osFamily) {
        // This website holds a collection of system property values under many OSes:
        // http://lopica.sourceforge.net/os.html

        if (osFamily == OsFamily.WINDOWS) {
            switch (osNameProp) {
                case "Windows 95":
                    return WINDOWS_95;
                case "Windows 98":
                    return WINDOWS_98;
                case "Windows Me":
                    return WINDOWS_ME;
                case "Windows NT":
                    return WINDOWS_NT;
                case "Windows 2000":
                    return WINDOWS_2000;
                case "Windows XP":
                    return WINDOWS_XP;
                case "Windows 2003":
                    return WINDOWS_2003;
                case "Windows Vista":
                    return WINDOWS_VISTA;
                case "Windows 7":
                    return WINDOWS_7;
                case "Windows 8":
                    return WINDOWS_8;
                case "Windows 8.1":
                    return WINDOWS_8_1;
            }
            // Newer version we don't know of yet, assume latest supported OS version
            return WINDOWS_10;
        }
        // Mac OS X versions
        if (osFamily == OsFamily.MAC_OS_X) {
            if (osVersionProp.startsWith("10.16")) {
                return MAC_OS_X_10_16;
            } else if (osVersionProp.startsWith("10.15")) {
                return MAC_OS_X_10_15;
            } else if (osVersionProp.startsWith("10.14")) {
                return MAC_OS_X_10_14;
            } else if (osVersionProp.startsWith("10.13")) {
                return MAC_OS_X_10_13;
            } else if (osVersionProp.startsWith("10.12")) {
                return MAC_OS_X_10_12;
            } else if (osVersionProp.startsWith("10.11")) {
                return MAC_OS_X_10_11;
            } else if (osVersionProp.startsWith("10.10")) {
                return MAC_OS_X_10_10;
            } else if (osVersionProp.startsWith("10.9")) {
                return MAC_OS_X_10_9;
            } else if (osVersionProp.startsWith("10.8")) {
                return MAC_OS_X_10_8;
            } else if (osVersionProp.startsWith("10.7")) {
                return MAC_OS_X_10_7;
            } else if (osVersionProp.startsWith("10.6")) {
                return MAC_OS_X_10_6;
            } else if (osVersionProp.startsWith("10.5")) {
                return MAC_OS_X_10_5;
            } else if (osVersionProp.startsWith("10.4")) {
                return MAC_OS_X_10_4;
            } else if (osVersionProp.startsWith("10.3")) {
                return MAC_OS_X_10_3;
            } else if (osVersionProp.startsWith("10.2")) {
                return MAC_OS_X_10_2;
            } else if (osVersionProp.startsWith("10.1")) {
                return MAC_OS_X_10_1;
            } else if (osVersionProp.startsWith("10.0")) {
                return MAC_OS_X_10_0;
            }
            // Newer version we don't know of yet, assume latest supported OS version
            return MAC_OS_X_10_16;
        }

        return OsVersion.UNKNOWN_VERSION;
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
    public boolean isCurrentOrLower() {
		return currentValue.compareTo(this) <= 0;
	}

	@Override
	public boolean isCurrentLower() {
		return currentValue.compareTo(this) < 0;
	}

	@Override
	public boolean isCurrentOrHigher() {
		return currentValue.compareTo(this) >= 0;
	}

	@Override
	public boolean isCurrentHigher() {
		return currentValue.compareTo(this) > 0;
	}

    @Override
    public String toString() {
        return stringRepresentation;
    }
}
