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

package com.jitlogic.zorka.common.tracedata;

import java.io.IOException;
import java.io.Serializable;

/**
 * Represents one stack trace element (eg. from symbolic form of exception).
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class SymbolicStackElement implements SymbolicRecord, Serializable {

    /**
     * ID for class name symbol
     */
    private int classId;

    /**
     * ID for method name symbol
     */
    private int methodId;

    /**
     * ID for file name symbol
     */
    private int fileId;

    /**
     * ID for line number symbol
     */
    private int lineNum;


    /**
     * Creates symbolic stack trace element from java StackTraceElement object
     *
     * @param ste     original stack trace element
     * @param symbols symbol registry
     */
    public SymbolicStackElement(StackTraceElement ste, SymbolRegistry symbols) {
        this.classId = symbols.symbolId(ste.getClassName());
        this.methodId = symbols.symbolId(ste.getMethodName());
        this.fileId = symbols.symbolId(ste.getFileName());
        this.lineNum = ste.getLineNumber();
    }


    /**
     * Creates symbolic stack trace element from scrach.
     *
     * @param classId  class name symbol ID
     * @param methodId method name symbol ID
     * @param fileId   file name symbol ID
     * @param lineNum  line number symbol ID
     */
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
            SymbolicStackElement sse = (SymbolicStackElement) ref;
            return classId == sse.classId && methodId == sse.methodId && fileId == sse.fileId && lineNum == sse.lineNum;
        } else {
            return false;
        }
    }


    @Override
    public String toString() {
        return "[" + classId + "," + methodId + "," + fileId + "," + lineNum + "]";
    }


    @Override
    public void traverse(MetadataChecker checker) throws IOException {
        classId = checker.checkSymbol(classId, this);
        methodId = checker.checkSymbol(methodId, this);
        fileId = checker.checkSymbol(fileId, this);
    }
}
