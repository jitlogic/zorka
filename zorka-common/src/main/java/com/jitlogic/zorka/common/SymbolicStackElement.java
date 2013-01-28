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

package com.jitlogic.zorka.common;

public class SymbolicStackElement {

    private int classId;
    private int methodId;
    private int fileId;
    private int lineNum;


    public SymbolicStackElement(StackTraceElement ste, SymbolRegistry symbols) {
        this.classId = symbols.symbolId(ste.getClassName());
        this.methodId = symbols.symbolId(ste.getMethodName());
        this.fileId = symbols.symbolId(ste.getFileName());
        this.lineNum = ste.getLineNumber();
    }


    public SymbolicStackElement(int classId, int methodId, int fileId, int lineNum) {
        this.classId = classId;
        this.methodId = methodId;
        this.fileId = fileId;
        this.lineNum = lineNum;
    }


    public int getClassId() {
        return classId;
    }


    public int getMethodId() {
        return methodId;
    }


    public int getFileId() {
        return fileId;
    }


    public int getLineNum() {
        return lineNum;
    }


    @Override
    public int hashCode() {
        return 11 * classId + 17 * methodId + 33 * fileId + 7 * lineNum;
    }


    @Override
    public boolean equals(Object ref) {
        if (ref instanceof SymbolicStackElement) {
            SymbolicStackElement sse = (SymbolicStackElement)ref;
            return classId == sse.classId && methodId == sse.methodId && fileId == sse.fileId && lineNum == sse.lineNum;
        } else {
            return false;
        }
    }


    @Override
    public String toString() {
        return "[" + classId + "," + methodId + "," + fileId + "," + lineNum + "]";
    }
}
