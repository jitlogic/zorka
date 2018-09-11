/*
 * Copyright 2012-2018 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
 * <p/>
 * This is free software. You can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * <p/>
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this software. If not, see <http://www.gnu.org/licenses/>.
 */

package com.jitlogic.zorka.core.test.spy.support.cbor;


import com.jitlogic.zorka.cbor.ByteArrayCborInput;
import com.jitlogic.zorka.common.tracedata.SymbolRegistry;
import com.jitlogic.zorka.core.spy.st.STraceBufChunk;
import com.jitlogic.zorka.cbor.CborDataReader;


import java.io.IOException;

public class STTrcTestUtils {

    public static int chunksCount(STraceBufChunk chunks) {
        int count = 0;
        for (STraceBufChunk c = chunks; c != null; c = c.getNext()) {
            count++;
        }
        return count;
    }

    public static int chunksLength(STraceBufChunk chunks) {
        int len = 0;
        for (STraceBufChunk c  = chunks; c != null; c = c.getNext()) {
            len = Math.max(len, c.getExtOffset()+c.getPosition());
        }
        return len;
    }

    public static byte[] chunksMerge(STraceBufChunk chunks) {
        byte[] buf = new byte[chunksLength(chunks)];

        for (STraceBufChunk c = chunks; c != null; c = c.getNext()) {
            System.arraycopy(c.getBuffer(), 0, buf, c.getExtOffset(), c.getPosition());
        }

        return buf;
    }

    public static Object decodeTrace(STraceBufChunk chunks) throws IOException {
        ByteArrayCborInput input = new ByteArrayCborInput(chunksMerge(chunks));
        return new CborDataReader(input, new TestTagProcessor(), new TestValResolver()).read();
    }

    public static STRec parseTrace(STraceBufChunk chunks, SymbolRegistry symbols) throws IOException {
        ByteArrayCborInput input = new ByteArrayCborInput(chunksMerge(chunks));
        STRec tr = (STRec) (new CborDataReader(input, new STTagProcessor(symbols), new TestValResolver()).read());
        tr.promoteUpAttrs();
        return tr;
    }

    public static String mkString(int len) {
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append('_');
        }
        return sb.toString();
    }


}
