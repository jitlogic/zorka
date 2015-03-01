package com.jitlogic.zorka.common.tracedata;

import java.io.IOException;

/**
 * Copyright 2012-2015 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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

public class MethodCallCounterRecord implements SymbolicRecord {

    private int classId, methodId, signatureId;

    private long nCalls;

    public MethodCallCounterRecord(int classId, int methodId, int signatureId, long nCalls) {
        this.classId = classId;
        this.methodId = methodId;
        this.signatureId = signatureId;
        this.nCalls = nCalls;
    }


    public int getClassId() {
        return classId;
    }


    public int getMethodId() {
        return methodId;
    }


    public int getSignatureId() {
        return signatureId;
    }


    public long getnCalls() {
        return nCalls;
    }


    @Override
    public void traverse(MetadataChecker checker) throws IOException {
        checker.checkSymbol(classId, this);
        checker.checkSymbol(methodId, this);
        checker.checkSymbol(signatureId, this);
    }


    @Override
    public int hashCode() {
        return classId + methodId * 31 + signatureId * 17;
    }


    @Override
    public boolean equals(Object obj) {
        if (obj instanceof MethodCallCounterRecord) {
            MethodCallCounterRecord rec = (MethodCallCounterRecord) obj;
            return this.classId == rec.classId
                    && this.methodId == rec.methodId
                    && this.signatureId == rec.signatureId
                    && this.nCalls == rec.nCalls;
        } else {
            return false;
        }
    }


    @Override
    public String toString() {
        return "MCR(" + classId + "," + methodId + "," + signatureId + "," + nCalls + ")";
    }
}
