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

package com.jitlogic.zorka.tracer;


import com.jitlogic.zorka.util.ByteBuffer;

public class SimpleTraceFormat extends TraceEventHandler {

    public static final int MAGIC_V1=0x57aace01;

    public static final byte TRACE_BEGIN  = 0x01;
    public static final byte TRACE_ENTER  = 0x02;
    public static final byte TRACE_RETURN = 0x03;
    public static final byte TRACE_ERROR  = 0x04;
    public static final byte TRACE_STATS  = 0x05;
    public static final byte NEW_SYMBOL   = 0x06;

    public static final byte NULL_ATTR    = 0x40;
    public static final byte BYTE_ATTR    = 0x41;
    public static final byte SHORT_ATTR   = 0x42;
    public static final byte INTEGER_ATTR = 0x43;
    public static final byte LONG_ATTR    = 0x44;
    public static final byte STRING_ATTR  = 0x45;

    private ByteBuffer buf;

    public SimpleTraceFormat(ByteBuffer buf) {
        this.buf = buf;
    }

    public SimpleTraceFormat(byte[] buf) {
        this.buf = new ByteBuffer(buf);
    }


    @Override
    public void traceBegin(int traceId, long clock) {
        buf.putByte(TRACE_BEGIN);
        buf.putInt(traceId);
        buf.putLong(clock);
    }


    @Override
    public void traceEnter(int classId, int methodId, int signatureId, long tstamp) {
        buf.putByte(TRACE_ENTER);
        buf.putInt(classId);
        buf.putInt(methodId);
        buf.putInt(signatureId);
        buf.putLong(tstamp);
    }


    @Override
    public void traceReturn(long tstamp) {
        buf.putByte(TRACE_RETURN);
        buf.putLong(tstamp);
    }


    @Override
    public void traceError(TracedException exception, long tstamp) {
        buf.putByte(TRACE_ERROR);
        if (exception instanceof SymbolicException) {
            encodeException((SymbolicException)exception);
        } else {
            throw new IllegalStateException("Cannot serialize local exception");
        }
        buf.putLong(tstamp);
    }


    @Override
    public void traceStats(long calls, long errors) {
        buf.putByte(TRACE_STATS);
        buf.putLong(calls);
        buf.putLong(errors);
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


    public void decode(TraceEventHandler output) {
        while (!buf.eof()) {
            byte cmd = buf.getByte();
            switch (cmd) {
                case TRACE_BEGIN:
                    output.traceBegin(buf.getInt(), buf.getLong());
                    break;
                case TRACE_ENTER:
                    output.traceEnter(buf.getInt(), buf.getInt(), buf.getInt(), buf.getLong());
                    break;
                case TRACE_RETURN:
                    output.traceReturn(buf.getLong());
                    break;
                case TRACE_ERROR:
                    output.traceError(decodeException(), buf.getLong());
                    break;
                case TRACE_STATS:
                    output.traceStats(buf.getLong(), buf.getLong());
                    break;
                case NEW_SYMBOL:
                    output.newSymbol(buf.getInt(), buf.getString());
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


    public void encodeException(SymbolicException ex) {
        buf.putInt(ex.getClassId());
        buf.putString(ex.getMessage());
    }


    public SymbolicException decodeException() {
        return new SymbolicException(buf.getInt(), buf.getString());
    }
}
