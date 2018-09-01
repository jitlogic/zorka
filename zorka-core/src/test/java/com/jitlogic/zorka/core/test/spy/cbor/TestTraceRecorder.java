/**
 * Copyright 2012-2016 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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

package com.jitlogic.zorka.core.test.spy.cbor;

import com.jitlogic.zorka.common.tracedata.SymbolRegistry;
import com.jitlogic.zorka.core.spy.st.STraceBufManager;
import com.jitlogic.zorka.core.spy.st.STraceBufOutput;
import com.jitlogic.zorka.core.spy.st.STraceHandler;

public class TestTraceRecorder extends STraceHandler {

    public TestTraceRecorder(STraceBufManager bufManager, SymbolRegistry symbols, STraceBufOutput output) {
        super(bufManager, symbols, output);
    }

    public long t, c;
    protected long ticks() {
        return t;
    }
    protected long clock() { return c; }
}
