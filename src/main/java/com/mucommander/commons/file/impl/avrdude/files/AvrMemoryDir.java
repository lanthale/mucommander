/*
 * This file is part of trolCommander, http://www.trolsoft.ru/en/soft/trolcommander
 * Copyright (C) 2013-2016 Oleg Trifonov
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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.mucommander.commons.file.impl.avrdude.files;

import com.mucommander.commons.file.AbstractFile;
import com.mucommander.commons.file.FilePermissions;
import com.mucommander.commons.file.FileURL;

import java.io.IOException;

/**
 * @author Oleg Trifonov
 * Created on 25/03/16.
 */
public class AvrMemoryDir extends AvrdudeFile {

    private AvrdudeFile parent;

    public AvrMemoryDir(FileURL url) throws IOException {
        super(url);
    }


    @Override
    public FilePermissions getPermissions() {
        return FilePermissions.DEFAULT_DIRECTORY_PERMISSIONS;
    }

    @Override
    public boolean isDirectory() {
        return true;
    }

    @Override
    public AbstractFile[] ls() throws IOException {
        AbstractFile[] files = new AbstractFile[2];
        files[0] = new AvrMemoryFile(FileURL.getFileURL(getURL() + "/" + getURL().getFilename()+ ".hex"));
        files[1] = new AvrMemoryFile(FileURL.getFileURL(getURL() + "/" + getURL().getFilename() + ".bin"));
        return files;
    }

    @Override
    public void mkdir() throws IOException {

    }

    @Override
    public boolean exists() {
        return true;
    }

    @Override
    public AbstractFile getParent() {
        if (parent == null) {
            try {
                parent = new AvrDeviceDir(getURL().getParent());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return parent;
    }
}
