/*
 * Copyright 2016-2019 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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

package com.jitlogic.zorka.common.codec;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class ExceptionData {

    int id;

    int classId;

    int msgId;

    String msg;

    int causeId;

    List<StackData> stackTrace = new ArrayList<StackData>();

    public ExceptionData(int id, int classId, String msg, int causeId) {
        this.id = id;
        this.classId = classId;
        this.msg = msg;
        this.causeId = causeId;
    }


    public ExceptionData(int id, int classId, int msgId, int causeId) {
        this.id = id;
        this.classId = classId;
        this.msgId = msgId;
        this.causeId = causeId;
    }

    public void addStackElement(StackData sel) {
        stackTrace.add(sel);
    }

    public List<StackData> getStackTrace() {
        return stackTrace;
    }

    public int getId() {
        return id;
    }

    public int getClassId() {
        return classId;
    }

    public int getMsgId() {
        return msgId;
    }

    public String getMsg() {
        return msg;
    }

    public int getCauseId() {
        return causeId;
    }

    @Override
    public String toString() {
        return "E{c=" + classId + ", m=" + msgId + ", c=" + causeId + ", s=" + stackTrace + "}";
    }
}
