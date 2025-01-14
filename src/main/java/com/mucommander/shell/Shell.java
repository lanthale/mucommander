/*
 * This file is part of muCommander, http://www.mucommander.com
 * Copyright (C) 2002-2012 Maxence Bernard
 *
 * muCommander is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * muCommander is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.mucommander.shell;

import java.io.IOException;

import com.mucommander.process.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mucommander.command.Command;
import com.mucommander.commons.conf.ConfigurationEvent;
import com.mucommander.commons.conf.ConfigurationListener;
import com.mucommander.commons.file.AbstractFile;
import com.mucommander.commons.file.impl.local.LocalFile;
import com.mucommander.conf.TcConfigurations;
import com.mucommander.conf.TcPreference;
import com.mucommander.conf.TcPreferences;
import com.mucommander.desktop.DesktopManager;

/**
 * Used to execute shell commands.
 * @author Maxence Bernard, Nicolas Rinaudo
 */
public class Shell implements ConfigurationListener {
	private static final Logger LOGGER = LoggerFactory.getLogger(Shell.class);
	
    // - Class variables -----------------------------------------------------
    // -----------------------------------------------------------------------
    /** Encoding used by the shell. */
    private static String   encoding;
    /** Whether encoding should be auto-detected or not. */
    private static boolean  autoDetectEncoding;
    /** Tokens that compose the shell command. */
    private static String[] tokens;
    /** Tokens that compose remote shell commands. */
    private static final String[] remoteTokens;
    /** Instance of configuration listener. */
    private static final Shell confListener;


    static {
    	TcConfigurations.addPreferencesListener(confListener = new Shell());

        // This could in theory also be written without the confListener reference.
        // It turns out, however, that proGuard is a bit too keen when removing fields
        // he thinks are not used. This code is written that way to make sure
        // confListener is not taken out, and the ConfigurationListener instance removed
        // instantly as there is only a WeakReference on it.
        // The things we have to do...
        Shell.setShellCommand();

        remoteTokens = new String[1];
    }

    /**
     * Prevents instances of Shell from being created.
     */
    private Shell() {}



    // - Shell interaction ---------------------------------------------------
    // -----------------------------------------------------------------------
    /**
     * Executes the specified command in the specified folder.
     * <p>
     * The <code>currentFolder</code> folder parameter will only be used if it's neither a
     * remote directory nor an archive. Otherwise, the command will init from the user's
     * home directory.
     *
     * @param     command       command to init.
     * @param     currentFolder where to init the command from.
     * @return                  the resulting process.
     * @exception IOException   thrown if any error occurs while trying to init the command.
     */
    public static AbstractProcess execute(String command, AbstractFile currentFolder) throws IOException {
        return execute(command, currentFolder, null);
    }

    /**
     * Executes the specified command in the specified folder.
     * <p>
     * The <code>currentFolder</code> folder parameter will only be used if it's neither a
     * remote directory nor an archive. Otherwise, the command will init from the user's
     * home directory.
     * <p>
     * Information about the resulting process will be sent to the specified <code>listener</code>.
     *
     * @param     command       command to init.
     * @param     currentFolder where to init the command from.
     * @param     listener      where to send information about the resulting process.
     * @return                  the resulting process.
     * @exception IOException   thrown if any error occurs while trying to init the command.
     */
    public static synchronized AbstractProcess execute(String command, AbstractFile currentFolder, ProcessListener listener) throws IOException {
        LOGGER.debug("Executing " + command);

        // Adds the command to history.
        ShellHistoryManager.add(command);

        // Builds the shell command.
        // Local files use the configuration defined shell. Remote files
        // will execute the command as-is.
        String[] commandTokens;
        if (currentFolder == null || currentFolder.hasAncestor(LocalFile.class)) {
            tokens[tokens.length - 1] = command;
            commandTokens = tokens;
        } else {
            remoteTokens[0] = command;
            commandTokens  = remoteTokens;
        }

        // Starts the process.
        if (autoDetectEncoding) {
            if (listener == null) {
                listener = new ShellEncodingListener();
            } else {
                ProcessListenerList listeners = new ProcessListenerList();
                listeners.add(listener);
                listeners.add(new ShellEncodingListener());
                listener = listeners;
            }
        }
        return (encoding == null) ? ProcessRunner.execute(commandTokens, currentFolder, listener) : ProcessRunner.execute(commandTokens, currentFolder, listener, encoding);
    }



    // - Configuration management --------------------------------------------
    // -----------------------------------------------------------------------
    /**
     * Extracts the shell command from configuration.
     */
    private static synchronized void setShellCommand() {
        String shellCommand;

        // Retrieves the configuration defined shell command.
        if (TcConfigurations.getPreferences().getVariable(TcPreference.USE_CUSTOM_SHELL, TcPreferences.DEFAULT_USE_CUSTOM_SHELL))
            shellCommand = TcConfigurations.getPreferences().getVariable(TcPreference.CUSTOM_SHELL, DesktopManager.getDefaultShell());
        else
            shellCommand = DesktopManager.getDefaultShell();

        // Splits the command into tokens, leaving room for the argument.
        String[] buffer = Command.getTokens(shellCommand);
        tokens = new String[buffer.length + 1];
        System.arraycopy(buffer, 0, tokens, 0, buffer.length);

        // Retrieves encoding configuration.
        encoding = TcConfigurations.getPreferences().getVariable(TcPreference.SHELL_ENCODING);
        autoDetectEncoding = TcConfigurations.getPreferences().getVariable(TcPreference.AUTODETECT_SHELL_ENCODING, TcPreferences.DEFAULT_AUTODETECT_SHELL_ENCODING);
    }

    /**
     * Reacts to configuration changes.
     */
    public void configurationChanged(ConfigurationEvent event) {
        if (event.getVariable().startsWith(TcPreferences.SHELL_SECTION)) {
            setShellCommand();
        }
    }

}
