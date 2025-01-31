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
 * This class represents a major version of Java, like <code>Java 1.5</code> for instance. The current runtime instance
 * is determined using the value of the <code>java.version</code> system property.
 * Being a {@link com.mucommander.commons.runtime.ComparableRuntimeProperty}, versions of Java are ordered and can be compared
 * against each other.
 *
 * @author Maxence Bernard, Arik Hadas
*/
public enum JavaVersion implements ComparableRuntimeProperty {
	/** Java 1.0.x */
    JAVA_1_0("1.0"),
    /** Java 1.1.x */
    JAVA_1_1("1.1"),
    /** Java 1.2.x */
    JAVA_1_2("1.2"),
    /** Java 1.3.x */
    JAVA_1_3("1.3"),
    /** Java 1.4.x */
    JAVA_1_4("1.4"),
    /** Java 1.5.x */
    JAVA_1_5("1.5"),
    /** Java 1.6.x */
    JAVA_1_6("1.6"),
    /** Java 1.7.x */
    JAVA_1_7("1.7"),
    /** Java 1.8.x */
    JAVA_1_8("1.8"),
    /** Java 1.9.x */
    JAVA_1_9("1.9"),
    /** Java 1.10.x */
    JAVA_1_10("1.10"),
    /** Java 1.9.x */
    JAVA_1_11("1.11");


    /** Logger used by this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(JavaVersion.class);

    /** Holds the JavaVersion of the current runtime environment  */
    private static final JavaVersion CURRENT_VALUE;

    /** Holds the String representation of the current JVM architecture  */
    private static final String CURRENT_ARCHITECTURE;

    /** The String representation of this RuntimeProperty, set at creation time */
    private final String stringRepresentation;

    static {
    	CURRENT_VALUE = parseSystemProperty(getRawSystemProperty());
    	CURRENT_ARCHITECTURE = System.getProperty("os.arch");
    	LOGGER.info("Current Java version: {}", CURRENT_VALUE);
    	LOGGER.info("Current JVM architecture: {}", CURRENT_ARCHITECTURE);
    }


    JavaVersion(String stringRepresentation) {
    	this.stringRepresentation = stringRepresentation;
    }


    /**
     * Returns <code>true</code> if the JVM architecture is amd64
     *
     * @return <code>true</code> if the JVM architecture is amd64, and <code>false</code> otherwise.
     */
    public static boolean isAmd64Architecture() {
    	return "amd64".equals(CURRENT_ARCHITECTURE);
    }

    /**
     * Returns the Java version of the current runtime environment.
     *
     * @return the Java version of the current runtime environment
     */
    public static JavaVersion getCurrent() {
        return CURRENT_VALUE;
    }

    /**
     * Returns the value of the system property which serves to detect the Java version at runtime.
     *
     * @return the value of the system property which serves to detect the Java version at runtime.
     */
    public static String getRawSystemProperty() {
        return System.getProperty("java.version");
    }

    /**
     * Returns a <code>JavaVersion</code> instance corresponding to the specified system property's value.
     *
     * @param javaVersionProp the value of the "java.version" system property
     * @return a JavaVersion instance corresponding to the specified system property's value
     */
    static JavaVersion parseSystemProperty(String javaVersionProp) {
        // Java version property should never be null or empty, but better be safe than sorry ...
        if (javaVersionProp == null || (javaVersionProp = javaVersionProp.trim()).isEmpty()) {
            // Assume last java version
            return values()[values().length-1];
        }
        for (JavaVersion ver : values()) {
            if (javaVersionProp.startsWith(ver.stringRepresentation)) {
                return ver;
            }
        }
        // Newer version we don't know of yet, assume the latest supported Java version
        return JavaVersion.JAVA_1_11;
    }

    /**
     * Returns <code>true</code> if this instance is the same instance as the one returned by {@link #getCurrent()}.
     *
     * @return true if this instance is the same as the current runtime value
     */
    public boolean isCurrent() {
        return this == CURRENT_VALUE;
    }


    @Override
	public boolean isCurrentOrLower() {
		return CURRENT_VALUE.compareTo(this) <= 0;
	}

	@Override
	public boolean isCurrentLower() {
		return CURRENT_VALUE.compareTo(this) < 0;
	}

	@Override
	public boolean isCurrentOrHigher() {
		return CURRENT_VALUE.compareTo(this) >= 0;
	}

	@Override
	public boolean isCurrentHigher() {
		return CURRENT_VALUE.compareTo(this) > 0;
	}

    @Override
    public String toString() {
        return stringRepresentation;
    }
}
