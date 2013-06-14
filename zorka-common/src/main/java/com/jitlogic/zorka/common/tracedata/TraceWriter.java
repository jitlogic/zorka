/**
 * Copyright 2012-2013 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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
package com.jitlogic.zorka.common.tracedata;

import java.io.IOException;

/**
 * Trace writer can be used to serialize trace data and send them to output
 * stream server by trace output passed to the writer.
 */
public interface TraceWriter {

    /** Serializes and writes trace data object. */
    void write(SymbolicRecord record) throws IOException;

    /** Sets trace output for the writer. */
    void setOutput(TraceOutput output);

    /**
     * Resets internal state of a trace writer. This clears internal state
     * of the writer and causes it to obtain new output stream from supplied
     * trace output object.
     */
    void reset();
}
