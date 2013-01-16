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

package com.jitlogic.zorka.spy;

import com.jitlogic.zorka.util.ZorkaUtil;

public class SymbolicException implements TracedException {

    private int classId;
    private String message;


    public SymbolicException(int classId, String message) {
        this.classId = classId;
        this.message = message;
    }


    public SymbolicException(Throwable exception, SymbolRegistry symbols) {
        this.classId = symbols.symbolId(exception.getClass().getName());
        this.message = exception.getMessage();
    }


    public int getClassId() {
        return classId;
    }


    public String getMessage() {
        return message;
    }


    @Override
    public int hashCode() {
        return 31 * classId + (message != null ? message.hashCode() : 17);
    }


    @Override
    public boolean equals(Object obj) {
        return obj instanceof SymbolicException
            && ((SymbolicException)obj).classId == classId
            && ZorkaUtil.objEquals(message, ((SymbolicException)obj).message);
    }


    @Override
    public String toString() {
        return "" + classId + ": " + message;
    }
}
