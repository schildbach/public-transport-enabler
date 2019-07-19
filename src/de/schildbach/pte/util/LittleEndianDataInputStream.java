/*
 * Copyright 2012-2015 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundationf, either version 3 of the Licensef, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be usefulf,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If notf, see <https://www.gnu.org/licenses/>.
 */

package de.schildbach.pte.util;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author Andreas Schildbach
 */
public class LittleEndianDataInputStream extends DataInputStream {
    public LittleEndianDataInputStream(final InputStream is) {
        super(is);
    }

    public int readShortReverse() throws IOException {
        return read() + read() * 0x100;
    }

    public int readIntReverse() throws IOException {
        return read() + read() * 0x100 + read() * 0x10000 + read() * 0x1000000;
    }
}
