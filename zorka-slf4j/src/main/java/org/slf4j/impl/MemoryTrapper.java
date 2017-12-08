package org.slf4j.impl;

import java.util.ArrayList;
import java.util.List;

public class MemoryTrapper implements ZorkaTrapper {

    public  static class TrapperMessage {
        private final ZorkaLogLevel logLevel;
        private final String tag;
        private final String msg;
        private final Throwable e;
        private final Object[] args;

        TrapperMessage(ZorkaLogLevel logLevel, String tag, String msg, Throwable e, Object[] args) {
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
}
