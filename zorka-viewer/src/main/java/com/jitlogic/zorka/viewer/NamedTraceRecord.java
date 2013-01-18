/**
 * Copyright 2012 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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

package com.jitlogic.zorka.viewer;

import com.jitlogic.zorka.spy.SymbolRegistry;
import com.jitlogic.zorka.spy.TraceRecord;

import java.util.Date;

public class NamedTraceRecord extends TraceRecord {

    private SymbolRegistry symbols;

    public NamedTraceRecord(SymbolRegistry symbols, NamedTraceRecord parent) {
        super(parent);
        this.symbols = symbols;
    }

    public NamedTraceRecord getParent() {
        return (NamedTraceRecord)super.getParent();
    }

    public String getTraceName() {
        return symbols.symbolName(getTraceId());
    }

    public String getClassName() {
        return symbols.symbolName(getClassId());
    }

    public String getMethodName() {
        return symbols.symbolName(getMethodId());
    }

    public String getSignature() {
        return symbols.symbolName(getSignatureId());
    }

    public Date getClockDt() {
        return new Date(getClock());
    }

    public SymbolRegistry getSymbols() {
        return symbols;
    }
}
