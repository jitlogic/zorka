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

package com.jitlogic.zorka.common;


/**
 * Implements simple trace encoder/decoder. Uses simple, uncompressed binary format.
 * It basically reads/writes stream of commands that correspond to methods of
 * TraceEventHandler interface.
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class SimpleTraceFormat extends TraceEventHandler {

    /** Simple format version 1 magic number */
    public static final int MAGIC_V1=0x57aace01;

    /** traceBegin() call */
    public static final byte TRACE_BEGIN  = 0x01;

    /** traceEnter() call */
    public static final byte TRACE_ENTER  = 0x02;

    /** traceReturn() call */
    public static final byte TRACE_RETURN = 0x03;

    /** traceError() call */
    public static final byte TRACE_ERROR  = 0x04;

    /** traceStats() call */
    public static final byte TRACE_STATS  = 0x05;

    /** newSymbol() call */
    public static final byte NEW_SYMBOL   = 0x06;

    /** longVal() call */
    public static final byte LONG_VAL     = 0x30;

    /** intVal() call */
    public static final byte INT_VAL      = 0x31;

    /** doubleVal() call */
    public static final byte DOUBLE_VAL   = 0x32;

    /** newAttr(key, null) call */
    public static final byte NULL_ATTR    = 0x40;

    /** newAttr(key, byteVal) call */
    public static final byte BYTE_ATTR    = 0x41;

    /** newAttr(key, shortVal) call */
    public static final byte SHORT_ATTR   = 0x42;

    /** newAttr(key, intVal) call */
    public static final byte INTEGER_ATTR = 0x43;

    /** newAttr(key, longVal) call */
    public static final byte LONG_ATTR    = 0x44;

    /** newAttr(key, stringVal) call */
    public static final byte STRING_ATTR  = 0x45;

    public static final byte NO_EXCEPTION = 0;

    public static final byte CAUSE_EXCEPTION = 1;


    /** Input/output buffer */
    private ByteBuffer buf;

    /**
     * Creates simple trace encoder.
     *
     * @param buf output buffer
     */
    public SimpleTraceFormat(ByteBuffer buf) {
        this.buf = buf;
    }

    /**
     * Creates simple trace decoder.
     *
     * @param buf input buffer
     */
    public SimpleTraceFormat(byte[] buf) {
        this.buf = new ByteBuffer(buf);
    }


    @Override
    public void traceBegin(int traceId, long clock, int flags) {
        buf.putByte(TRACE_BEGIN);
        buf.putInt(traceId);
        buf.putLong(clock);
        buf.putInt(flags);
    }


    @Override
    public void traceEnter(int classId, int methodId, int signatureId, long tstamp) {
        buf.putByte(TRACE_ENTER);
        buf.putInt(classId);
        buf.putInt(methodId);
        buf.putInt(signatureId);
    }


    @Override
    public void traceReturn(long tstamp) {
        buf.putByte(TRACE_RETURN);
        buf.putLong(tstamp);
    }


    @Override
    public void traceError(Object exception, long tstamp) {
        buf.putByte(TRACE_ERROR);
        if (exception instanceof SymbolicException) {
            encodeException((SymbolicException)exception);
        } else {
            throw new IllegalStateException("Cannot serialize local exception");
        }
        buf.putLong(tstamp);
    }


    @Override
    public void traceStats(long calls, long errors, int flags) {
        buf.putByte(TRACE_STATS);
        buf.putLong(calls);
        buf.putLong(errors);
        buf.putInt(flags);
    }


    @Override
    public void newSymbol(int symbolId, String symbolText) {
        buf.putByte(NEW_SYMBOL);
        buf.putInt(symbolId);
        buf.putString(symbolText);
    }


    @Override
    public void newAttr(int attrId, Object attrVal) {
        if (attrVal instanceof String) {
            buf.putByte(STRING_ATTR);
            buf.putInt(attrId);
            buf.putString(attrVal.toString());
        } else if (attrVal instanceof Long) {
            buf.putByte(LONG_ATTR);
            buf.putInt(attrId);
            buf.putLong((Long) attrVal);
        } else if (attrVal instanceof Integer) {
            buf.putByte(INTEGER_ATTR);
            buf.putInt(attrId);
            buf.putInt((Integer) attrVal);
        } else if (attrVal instanceof Byte) {
            buf.putByte(BYTE_ATTR);
            buf.putInt(attrId);
            buf.putByte((Byte) attrVal);
        } else if (attrVal instanceof Short) {
            buf.putByte(SHORT_ATTR);
            buf.putInt(attrId);
            buf.putShort((Short) attrVal);
        } else {
            buf.putByte(NULL_ATTR);
            buf.putInt(attrId);
        }
    }

    @Override
    public void longVals(long clock, int objId, int[] components, long[] values) {
        buf.putByte(LONG_VAL);
        buf.putLong(clock);
        buf.putInt(objId);
        buf.putInt(components.length);
        for (int i = 0; i < components.length; i++) {
            buf.putInt(components[i]);
            buf.putLong(values[i]);
        }
    }

    @Override
    public void intVals(long clock, int objId, int[] components, int[] values) {
        buf.putByte(INT_VAL);
        buf.putLong(clock);
        buf.putInt(objId);
        buf.putInt(components.length);
        for (int i = 0; i < components.length; i++) {
            buf.putInt(components[i]);
            buf.putInt(values[i]);
        }
    }

    @Override
    public void doubleVals(long clock, int objId, int[] components, double[] values) {
        buf.putByte(DOUBLE_VAL);
        buf.putLong(clock);
        buf.putInt(objId);
        buf.putInt(components.length);
        for (int i = 0; i < components.length; i++) {
            buf.putInt(components[i]);
            buf.putDouble(values[i]);
        }
    }


    /**
     * Decodes buffer content. Decoded elements are transformed
     * into calls to supplied trace event handler.
     *
     * @param output handler that will receive decoded events
     */
    public void decode(TraceEventHandler output) {
        while (!buf.eof()) {
            byte cmd = buf.getByte();
            switch (cmd) {
                case TRACE_BEGIN:
                    output.traceBegin(buf.getInt(), buf.getLong(), buf.getInt());
                    break;
                case TRACE_ENTER:
                    output.traceEnter(buf.getInt(), buf.getInt(), buf.getInt(), 0);
                    break;
                case TRACE_RETURN:
                    output.traceReturn(buf.getLong());
                    break;
                case TRACE_ERROR:
                    output.traceError(decodeException(), buf.getLong());
                    break;
                case TRACE_STATS:
                    output.traceStats(buf.getLong(), buf.getLong(), buf.getInt());
                    break;
                case NEW_SYMBOL:
                    output.newSymbol(buf.getInt(), buf.getString());
                    break;
                case LONG_VAL:
                    decodeLongVals(output);
                    break;
                case INT_VAL:
                    decodeIntVals(output);
                    break;
                case DOUBLE_VAL:
                    decodeDoubleVals(output);
                    break;
                case NULL_ATTR:
                    output.newAttr(buf.getInt(), null);
                    break;
                case BYTE_ATTR:
                    output.newAttr(buf.getInt(), buf.getByte());
                    break;
                case SHORT_ATTR:
                    output.newAttr(buf.getInt(), buf.getShort());
                    break;
                case INTEGER_ATTR:
                    output.newAttr(buf.getInt(), buf.getInt());
                    break;
                case LONG_ATTR:
                    output.newAttr(buf.getInt(), buf.getLong());
                    break;
                case STRING_ATTR:
                    output.newAttr(buf.getInt(), buf.getString());
                    break;
                default:
                    throw new IllegalArgumentException("Invalid prefix: " + cmd);
            }
        }
    }


    /**
     * Decodes a set of long values
     *
     * @param output
     */
    private void decodeLongVals(TraceEventHandler output) {
        long clock = buf.getLong();
        int objId = buf.getInt();
        int len = buf.getInt();

        int[] components = new int[len];
        long[] values = new long[len];

        for (int i = 0; i < len; i++) {
            components[i] = buf.getInt();
            values[i] = buf.getLong();
        }

        output.longVals(clock, objId, components, values);
    }


    private void decodeIntVals(TraceEventHandler output) {
        long clock = buf.getLong();
        int objId = buf.getInt();
        int len = buf.getInt();

        int[] components = new int[len];
        int[] values = new int[len];

        for (int i = 0; i < len; i++) {
            components[i] = buf.getInt();
            values[i] = buf.getInt();
        }

        output.intVals(clock, objId, components, values);
    }


    private void decodeDoubleVals(TraceEventHandler output) {
        long clock = buf.getLong();
        int objId = buf.getInt();
        int len = buf.getInt();

        int[] components = new int[len];
        double[] values = new double[len];

        for (int i = 0; i < len; i++) {
            components[i] = buf.getInt();
            values[i] = buf.getDouble();
        }

        output.doubleVals(clock, objId, components, values);
    }


    /**
     * Encodes exception object.
     *
     * @param ex (wrapped or synthetic) symbolic exception representation.
     */
    public void encodeException(SymbolicException ex) {
        buf.putInt(ex.getClassId());
        buf.putString(ex.getMessage());

        SymbolicStackElement[] stackTrace = ex.getStackTrace();
        buf.putShort(stackTrace.length);

        for (SymbolicStackElement el : stackTrace) {
            buf.putInt(el.getClassId());
            buf.putInt(el.getMethodId());
            buf.putInt(el.getFileId());
            buf.putInt(el.getLineNum());
        }

        if (ex.getCause() != null) {
            buf.putByte(CAUSE_EXCEPTION);
            encodeException(ex.getCause());
        } else {
            buf.putByte(NO_EXCEPTION);
        }
    }


    /**
     * Decodes exception object.
     *
     * @return (synthetic) symbolic exception representation.
     */
    public SymbolicException decodeException() {
        int classId = buf.getInt();
        String message = buf.getString();

        int stackLen = buf.getShort();
        SymbolicStackElement[] stack = new SymbolicStackElement[stackLen];

        for (int i = 0; i < stackLen; i++) {
            stack[i] = new SymbolicStackElement(buf.getInt(), buf.getInt(), buf.getInt(), buf.getInt());
        }

        int stop = buf.getByte();

        SymbolicException cause;

        if (stop == CAUSE_EXCEPTION) {
            cause = decodeException();
        } else {
            cause = null;
        }

        return new SymbolicException(classId, message, stack, cause);
    }
}
