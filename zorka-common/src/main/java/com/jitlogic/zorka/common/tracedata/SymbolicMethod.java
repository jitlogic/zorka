package com.jitlogic.zorka.common.tracedata;

import java.io.IOException;

public class SymbolicMethod implements SymbolicRecord {

    private final static long MDEF_MASK = 0x00000000001FFFFFL;

    private int classId;

    private int methodId;

    private int signatureId;

    public SymbolicMethod(long mdef) {
        classId = (int)(mdef & MDEF_MASK);
        methodId = (int)((mdef >> 21) & MDEF_MASK);
        signatureId = (int)((mdef >> 42) & MDEF_MASK);
    }

    public SymbolicMethod(int classId, int methodId, int signatureId) {
        this.classId = classId;
        this.methodId = methodId;
        this.signatureId = signatureId;
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

    public void setClassId(int classId) {
        this.classId = classId;
    }

    public void setMethodId(int methodId) {
        this.methodId = methodId;
    }

    public void setSignatureId(int signatureId) {
        this.signatureId = signatureId;
    }

    public long mdef() {
        return (classId & MDEF_MASK)
            | ((methodId & MDEF_MASK) << 21)
            | ((signatureId & MDEF_MASK) << 42);
    }

    @Override
    public void traverse(MetadataChecker checker) throws IOException {
        checker.checkSymbol(classId, this);
        checker.checkSymbol(methodId, this);
        checker.checkSymbol(signatureId, this);
    }

    @Override
    public int hashCode() {
        return 11 * classId + 139 * methodId + 997 * signatureId;
    }

    @Override
    public String toString() {
        return "M[" + classId + "," + methodId + "," + signatureId + "]";
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof SymbolicMethod)) return false;
        SymbolicMethod sm = (SymbolicMethod)obj;
        return classId == sm.classId && methodId == sm.methodId && signatureId == sm.signatureId;
    }
}
