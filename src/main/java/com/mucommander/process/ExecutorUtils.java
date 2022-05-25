/*
 * This file is part of trolCommander, http://www.trolsoft.ru/en/soft/trolcommander
 * Copyright (C) 2013-2015 Oleg Trifonov
 *
 * trolCommander is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * trolCommander is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.mucommander.process;

import com.mucommander.command.Command;
import com.mucommander.commons.file.AbstractFile;

import java.io.IOException;

/**
 * @author Oleg Trifonov
 * Created on 22/04/16.
 */
public class ExecutorUtils {

    /**
     * Executes the specified command in the specified folder.
     *
     * @param command                  command to execute
     * @param currentFolder            where to init the command from.
     * @param onFinish  here to send information about the resulting process.
     * @param encoding                 output encoding (system default is used if <code>null</code>).
     * @return process exit code
     * @throws IOException
     * @throws InterruptedException
     */
    public static int executeAndGetOutput(String command, AbstractFile currentFolder, ExecutionFinishListener onFinish,
                                           String encoding) throws IOException, InterruptedException {
        StringBuffer out = new StringBuffer();
        AbstractProcess process = execute(command, currentFolder, new ProcessListener() {
                @Override
                public void processDied(int returnValue) {}

                @Override
                public void processOutput(String output) {
                    out.append(output);
                }

                @Override
                public void processOutput(byte[] buffer, int offset, int length) {}
            }, encoding);
        int exitCode = process.waitFor();
        process.waitMonitoring();
        process.destroy();
        if (onFinish != null) {
            onFinish.onFinish(exitCode, out.toString());
        }
        return exitCode;
    }

    public static int executeAndGetOutput(String[] command, AbstractFile currentFolder, ExecutionFinishListener onFinish,
                                          String encoding) throws IOException, InterruptedException {
        StringBuffer out = new StringBuffer();
        AbstractProcess process = execute(command, currentFolder, new ProcessListener() {
            @Override
            public void processDied(int returnValue) {}

            @Override
            public void processOutput(String output) {
                out.append(output);
            }

            @Override
            public void processOutput(byte[] buffer, int offset, int length) {}
        }, encoding);
        int exitCode = process.waitFor();
        process.waitMonitoring();
        process.destroy();
        if (onFinish != null) {
            onFinish.onFinish(exitCode, out.toString());
        }
        return exitCode;
    }

    /**
     * Executes the specified command in the specified folder, using system default encoding.
     *
     * @param command                  command to execute
     * @param currentFolder            where to init the command from.
     * @param executionFinishListener  here to send information about the resulting process.
     * @return exit code
     * @throws IOException
     * @throws InterruptedException
     */
    public static int executeAndGetOutput(String command, AbstractFile currentFolder, ExecutionFinishListener executionFinishListener) throws IOException, InterruptedException {
        return executeAndGetOutput(command, currentFolder, executionFinishListener, null);
    }

    public static int executeAndGetOutput(String[] command, AbstractFile currentFolder, ExecutionFinishListener executionFinishListener) throws IOException, InterruptedException {
        return executeAndGetOutput(command, currentFolder, executionFinishListener, null);
    }

    public static int execute(String command, AbstractFile currentFolder) throws IOException, InterruptedException {
        return executeAndGetOutput(command, currentFolder, null);
    }

    public static int execute(String command) throws IOException, InterruptedException {
        return executeAndGetOutput(command, null, null);
    }

    public static int execute(String[] command) throws IOException, InterruptedException {
        return executeAndGetOutput(command, null, null);
    }


    private static AbstractProcess execute(String command, AbstractFile currentFolder, ProcessListener listener, String encoding) throws IOException {
        String[] tokens = Command.getTokens(command);
        return encoding == null ? ProcessRunner.execute(tokens, currentFolder, listener) : ProcessRunner.execute(tokens, currentFolder, listener, encoding);
    }

    private static AbstractProcess execute(String[] command, AbstractFile currentFolder, ProcessListener listener, String encoding) throws IOException {
        return encoding == null ? ProcessRunner.execute(command, currentFolder, listener) : ProcessRunner.execute(command, currentFolder, listener, encoding);
    }
}
