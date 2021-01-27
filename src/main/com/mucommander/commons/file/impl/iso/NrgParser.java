package com.mucommander.commons.file.impl.iso;

import com.mucommander.commons.file.AbstractFile;
import com.mucommander.commons.io.RandomAccessInputStream;

import java.io.IOException;
import java.util.List;
import java.util.Vector;

/**
 * @see <a href="http://en.wikipedia.org/wiki/NRG_(file_format)">NRG file format on Wikipedia</a>
 * @author Xavier Martin
 */
class NrgParser extends IsoParser {

    static List<IsoArchiveEntry> getEntries(byte[] buffer, AbstractFile file, RandomAccessInputStream rais) throws Exception {
        int sectSize = IsoUtil.MODE1_2048;

        // sector shift : 0 most of the time
        long sector_offset = 0;

        // bytes : depend if there's earlier track we discard
        long shiftOffset = 0;

        int len = rais.available();

        int[] tracksMode = new int[255];
        long[] tracksOffset = new long[255];
        long[] tracksStart = new long[255];
        long[] tracksEnd = new long[255];

        int tracks = 0;

        for (int i = 7; i <= 11; i += 4) {
            long offset = -1;

            rais.seek(len - i);

            if (rais.read(buffer, 0, (i == 7) ? 4 : 12) == -1)
                throw new IOException("unable to read tail of nrg file");

            if (buffer[0] == 'N' && buffer[1] == 'E' && buffer[2] == 'R' && buffer[3] == '5') // v2 footer
                offset = IsoUtil.toDword(buffer, 8);
            else if (buffer[0] == 'N' && buffer[1] == 'E' && buffer[2] == 'R' && buffer[3] == 'O') // v1 footer
                offset = IsoUtil.toDword(buffer, 4);

            if (offset == -1)
                continue;

            // read chunks
            boolean end = false;
            for (int j = 0; j < 255 && !end; j++) {
                long clen;

                rais.seek(offset);

                if (rais.read(buffer, 0, 8) == -1)
                    throw new IOException("unable to read chunk in tail of nrg file");

                if (buffer[0] == 'E' && buffer[1] == 'N' && buffer[2] == 'D' && buffer[3] == '!')
                    end = true;
                else if (buffer[0] == 'E' && buffer[1] == 'T' && buffer[2] == 'N') {
                    clen = IsoUtil.toDword(buffer, 4);
                    boolean ETN2 = buffer[3] == '2';

                    if (rais.read(buffer, 0, (int) clen) == -1)
                        throw new IOException("unable to read chunk in tail of nrg file");

                    for (int z = 0; z < clen; z += ETN2 ? 32 : 20) {
                        tracksOffset[tracks] = IsoUtil.toDword(buffer, ETN2 ? 4 : 0);
                        tracksMode[tracks] = IsoUtil.toDword(buffer, ETN2 ? 16 : 8);
                        tracks++;
                    }
                    end = true;
                } else if (buffer[0] == 'D' && buffer[1] == 'A' && buffer[2] == 'O') {
                    boolean DAOX = buffer[3] == 'X';
                    clen = IsoUtil.toDword(buffer, 4);
                    offset += 8 + clen;

                    // skip endian
                    if (rais.read(buffer, 0, 4) == -1)
                        throw new IOException("unable to skip endian in DAO chunk");

                    if (rais.read(buffer, 0, (int) clen) == -1)
                        throw new IOException("unable to read DAO chunk");

                    int first = buffer[16];
                    int cur = first - 1;
                    for (int z = 18; z < clen - 4; z += DAOX ? 42 : 30) {
                        tracksMode[cur] = buffer[z + 14];
                        tracksOffset[cur] = IsoUtil.toDword(buffer, DAOX ? z + 30 : 22);
                        tracksEnd[cur] = IsoUtil.toDword(buffer, DAOX ? z + 38 : 26) - 1;
                        cur++;
                    }
                    tracks = cur;
                    rais.seek(offset);
                    // TODO CUES
                } else if (buffer[0] == 'C' && buffer[1] == 'U' && buffer[2] == 'E' && buffer[3] == 'X') {
                    clen = IsoUtil.toDword(buffer, 4);
                    offset += 8 + clen;

                    if (rais.read(buffer, 0, (int) clen) == -1)
                        throw new IOException("unable to read CUEX chunk");

                    for (int z = 0; z < clen; z += 8) {
                        //long toc = IsoUtil.toDword(buffer, z);
                        long tstart = IsoUtil.toDword(buffer, z + 4);
                        if (buffer[z + 2] == 0 || (buffer[z + 1] & 0xff) == 0xAA) {
                            // skip toc/pregap ?
                        } else {
                            tracksStart[tracks] = tstart;
                            tracks++;
                        }
                    }
                    rais.seek(offset);
                } else {
                    // skip irrelevant chunk
                    clen = IsoUtil.toDword(buffer, 4);
                    offset += 8 + clen;
                    rais.seek(offset);
                }
            }

            boolean audioOnly = true;
            for (int k = 0; k < tracks; k++) {
                shiftOffset = tracksOffset[k];
                sector_offset = tracksStart[k];

                switch (tracksMode[k]) {
                    case 0:
                        sectSize = IsoUtil.MODE1_2048;
                        audioOnly = false;
                        break;
                    case 3:
                        sectSize = IsoUtil.MODE2_2336;
                        audioOnly = false;
                        break;
                    case 7:
                        // audio 2352 - 2352
                        sectSize = IsoUtil.MODE2_2352;
                        break;
                    case 16:
                        // TODO find a sample image w/ subchannel data
                        // audio 2448 - 2352
                        break;
                    default:
                        throw new Exception("unhandled mode " + tracksMode[k]);
                }
            }

            // fun : handle audio disc :)
            if (audioOnly) {
                List<IsoArchiveEntry> entries = new Vector<>();
                for (int k = 0; k < tracks; k++) {
                    entries.add(
                            new IsoArchiveEntry(
                                    file.getName() + ".TRACK" + (k + 1) + ".wav",
                                    false,
                                    file.getLastModifiedDate(),
                                    tracksEnd[k] - tracksOffset[k] + IsoUtil.WAV_header, // adding wav header
                                    0,
                                    sectSize,
                                    tracksOffset[k],
                                    true)
                    );
                }
                return entries;
            }

        }

        return getEntries(buffer, rais, sectSize, sector_offset, shiftOffset);

    }
}