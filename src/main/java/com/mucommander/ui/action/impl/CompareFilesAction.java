/*
 * This file is part of trolCommander, http://www.trolsoft.ru/en/soft/trolcommander
 * Copyright (C) 2013-2020 Oleg Trifonov
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
package com.mucommander.ui.action.impl;

import com.mucommander.commons.file.AbstractFile;
import com.mucommander.commons.file.FileOperation;
import com.mucommander.commons.file.filter.AbstractFileFilter;
import com.mucommander.commons.file.filter.AndFileFilter;
import com.mucommander.commons.file.filter.FileOperationFilter;
import com.mucommander.commons.file.impl.local.LocalFile;
import com.mucommander.commons.file.util.FileSet;
import com.mucommander.commons.runtime.OsFamily;
import com.mucommander.process.ExecutorUtils;
import com.mucommander.ui.action.AbstractActionDescriptor;
import com.mucommander.ui.action.ActionCategory;
import com.mucommander.ui.action.ActionDescriptor;
import com.mucommander.ui.action.TcAction;
import com.mucommander.ui.main.MainFrame;

import javax.swing.KeyStroke;
import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * Created on 01/02/16.
 * @author Oleg Trifonov
 */
public class CompareFilesAction extends SelectedFilesAction {

    private static final String OPENDIFF_PATH = "/usr/bin/opendiff";
    private static final String MELD_PATH = "/usr/bin/meld";

    public enum Method {
        MAC_OS_X_DIFF {
            @Override
            void exec(String file1, String file2) throws IOException, InterruptedException {
                ExecutorUtils.execute(new String[]{OPENDIFF_PATH,  file1, file2});
            }
        },
        LINUX_MELD {
            @Override
            void exec(String file1, String file2) throws IOException, InterruptedException {
                ExecutorUtils.execute(new String[]{MELD_PATH,  file1, file2});
            }
        };

        abstract void exec(String file1, String file2) throws IOException, InterruptedException;
    }

    private static Method method;

    private CompareFilesAction(MainFrame mainFrame, Map<String, Object> properties) {
        super(mainFrame, properties);
        setSelectedFileFilter(new AndFileFilter(
            new FileOperationFilter(FileOperation.READ_FILE),
            new AbstractFileFilter() {
                @Override
                public boolean accept(AbstractFile file) {
                    if (supported()) {
                        AbstractFile leftFile = mainFrame.getLeftPanel().getFileTable().getSelectedFile();
                        AbstractFile rightFile = mainFrame.getRightPanel().getFileTable().getSelectedFile();
                        return isLocalFile(leftFile) && isLocalFile(rightFile);
                    }
                    return false;
                }
            }
        ));
    }

    private static boolean isLocalFile(AbstractFile file) {
        return file != null && !file.isDirectory() && file instanceof LocalFile;
    }

    @Override
    public void performAction(FileSet files) {
        AbstractFile leftFile = mainFrame.getLeftPanel().getFileTable().getSelectedFile();
        AbstractFile rightFile = mainFrame.getRightPanel().getFileTable().getSelectedFile();
        if (leftFile == null || rightFile == null) {
            return;
        }
        String leftFilePath = leftFile.getAbsolutePath().replace(" ", "\\ ");
        String rightFilePath = rightFile.getAbsolutePath().replace(" ", "\\ ");
        compareTwoFiles(leftFilePath, rightFilePath);
    }

    public static void compareTwoFiles(String file1, String file2) {
        if (method == null || file1 == null || file2 == null) {
            return;
        }
        new Thread(() -> {
            try {
                method.exec(file1, file2);
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public static boolean supported() {
        if (method != null) {
            return true;
        }

        switch (OsFamily.getCurrent()) {
            case MAC_OS_X:
                if (new File(OPENDIFF_PATH).exists()) {
                    method = Method.MAC_OS_X_DIFF;
                    return true;
                }
                break;
            case LINUX:
                if (new File(MELD_PATH).exists()) {
                    method = Method.LINUX_MELD;
                    return true;
                }
                break;
        }
        return false;
    }

    @Override
    public ActionDescriptor getDescriptor() {
        return new Descriptor();
    }

    public static final class Descriptor extends AbstractActionDescriptor {
        public static final String ACTION_ID = "CompareFiles";

        public String getId() {
            return ACTION_ID;
        }

        public ActionCategory getCategory() {
            return ActionCategory.FILES;
        }

        public KeyStroke getDefaultAltKeyStroke() {
            return null;
        }

        public KeyStroke getDefaultKeyStroke() {
            return null;
        }

        public TcAction createAction(MainFrame mainFrame, Map<String, Object> properties) {
            return new CompareFilesAction(mainFrame, properties);
        }

    }
}