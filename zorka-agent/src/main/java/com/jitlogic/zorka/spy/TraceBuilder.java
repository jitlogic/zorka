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


/**
 * This class receives loose tracer submissions from single thread
 * and constructs traces.
 *
 *
 */
public class TraceBuilder implements TraceEventHandler {

    private long methodTime = 250000;

    private TraceEventHandler output;

    private TraceElement top = new TraceElement(null);



    public TraceBuilder(TraceEventHandler output) {
        this.output = output;
    }


    public TraceBuilder(TraceEventHandler output, long methodTime) {
        this.output = output;
        this.methodTime = methodTime;
    }


    @Override
    public void traceBegin(int traceId) {
        top.traceBegin(traceId);
    }


    @Override
    public void traceEnter(int classId, int methodId, int signatureId, long tstamp) {
        if (top.isBusy()) {
            top = new TraceElement(top);
        }
        top.traceEnter(classId, methodId, signatureId, tstamp);
    }


    @Override
    public void traceReturn(long tstamp) {

        while (!top.isBusy() && top.getParent() != null) {
            top = top.getParent();
        }

        top.traceReturn(tstamp);
        pop();
    }


    @Override
    public void traceError(TracedException exception, long tstamp) {

        while (!top.isBusy() && top.getParent() != null) {
            top = top.getParent();
        }

        top.traceError(exception, tstamp);
        pop();
    }


    @Override
    public void traceStats(long calls, long errors) {
        // Ignore this
    }


    @Override
    public void newSymbol(int symbolId, String symbolText) {
        // Ignore this
    }


    @Override
    public void newAttr(int parId, Object val) {
        top.newAttr(parId, val);
    }


    private void pop() {
        if (top.isTrace() && top.getTime() > methodTime) {
            top.traverse(output);
        }

        TraceElement parent = top.getParent();

        if (parent != null) {
            if (top.getTime() > methodTime || top.getErrors() > 0) {
                parent.addChild(top);
                top = parent;
            } else {
                parent.mergeChild(top);
                top.clean();
            }
        } else {
            top.clean();
        }
    }

}
