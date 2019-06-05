/*
 * Copyright 2012-2019 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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
package org.slf4j.impl;

import java.util.ArrayList;
import java.util.List;

public class MemoryTrapper implements ZorkaTrapper {

    public  static class TrapperMessage {
        private final long tstamp;
        private final ZorkaLogLevel logLevel;
        private final String tag;
        private final String msg;
        private final Throwable e;
        private final Object[] args;

        TrapperMessage(ZorkaLogLevel logLevel, String tag, String msg, Throwable e, Object[] args) {
            this.tstamp = System.currentTimeMillis();
            this.logLevel = logLevel;
            this.tag = tag;
            this.msg = msg;
            this.e = e;
            this.args = args;
        }

        public ZorkaLogLevel getLogLevel() {
            return logLevel;
        }

        public String getTag() {
            return tag;
        }

        public String getMsg() {
            return msg;
        }

        public Throwable getE() {
            return e;
        }

        public Object[] getArgs() {
            return args;
        }

        @Override
        public String toString() {
            return String.valueOf(logLevel) + ' ' + tag + ' ' + msg + ' ' + e;
        }
    }

    private List<TrapperMessage> messages = new ArrayList<TrapperMessage>(128);

    @Override
    public synchronized void trap(ZorkaLogLevel logLevel, String tag, String msg, Throwable e, Object... args) {
        messages.add(new TrapperMessage(logLevel, tag, msg, e, args));
    }

    public synchronized List<TrapperMessage> drain() {
        List<TrapperMessage> rslt = messages;
        messages = new ArrayList<TrapperMessage>(128);
        return rslt;
    }

    public synchronized List<TrapperMessage> getAll() {
        return new ArrayList<TrapperMessage>(messages);
    }
}
