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


import com.jitlogic.zorka.util.ZorkaAsyncThread;

/**
 * This class receives loose tracer submissions from single thread
 * and constructs traces.
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class TraceBuilder extends TraceEventHandler {

    private long methodTime = 250000;

    private ZorkaAsyncThread<TraceElement> output;

    private TraceElement top = new TraceElement(null);



    public TraceBuilder(ZorkaAsyncThread<TraceElement> output) {
        this.output = output;
    }


    public TraceBuilder(ZorkaAsyncThread<TraceElement> output, long methodTime) {
        this.output = output;
        this.methodTime = methodTime;
    }


    @Override
    public void traceBegin(int traceId, long clock) {
        top.setTraceRecord(new TraceRecord(top, traceId, top.getClock()));
        top.setTraceRecord(new TraceRecord(top, top.getTraceId(), clock));
    }


    @Override
    public void traceEnter(int classId, int methodId, int signatureId, long tstamp) {
        if (top.isBusy()) {
            top = new TraceElement(top);
        }

        top.setClassId(classId);
        top.setMethodId(methodId);
        top.setSignatureId(signatureId);
        top.setTime(tstamp);
        top.setCalls(top.getCalls() + 1);
    }


    @Override
    public void traceReturn(long tstamp) {

        while (!top.isBusy() && top.getParent() != null) {
            top = top.getParent();
        }

        top.setTime(tstamp-top.getTime());
        pop();
    }


    @Override
    public void traceError(TracedException exception, long tstamp) {

        while (!top.isBusy() && top.getParent() != null) {
            top = top.getParent();
        }

        top.setException(exception);
        top.setTime(tstamp-top.getTime());
        top.setErrors(top.getErrors() + 1);

        pop();
    }


    @Override
    public void newAttr(int attrId, Object attrVal) {
        top.setAttr(attrId, attrVal);
    }


    private void pop() {
        boolean clean = true;
        if (top.isTrace() && top.getTime() >= methodTime) {
            output.submit(top);
            clean = false;
        }

        TraceElement parent = top.getParent();

        if (parent != null) {
            if (top.getTime() > methodTime || top.getErrors() > 0) {
                parent.addChild(top);
                clean = false;
            }
            parent.setCalls(parent.getCalls() + top.getCalls());
            parent.setErrors(parent.getErrors() + top.getErrors());
        }

        if (clean) {
            top.clean();
        } else {
            top = parent != null ? parent : new TraceElement(null);
        }
    } // pop()

}
