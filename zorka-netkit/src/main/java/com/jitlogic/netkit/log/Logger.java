/*
 * Copyright (c) 2012-2019 Rafał Lewczuk All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jitlogic.netkit.log;

import com.jitlogic.netkit.NetCtx;
import com.jitlogic.netkit.TlsContext;

import java.nio.channels.SelectionKey;

import static com.jitlogic.netkit.log.LoggerFactory.*;
import static com.jitlogic.netkit.util.NetkitUtil.keyToString;

/**
 * HttpKit logging API has no dependencies and integrations are easy to implement. Battery included.
 * API is modeled after sfl4j, so it is easy to integrate with any other logger framework.
 */
public class Logger {


    private String tag;

    public Logger(String tag) {
        this.tag = tag;
    }

    public boolean isTraceEnabled() {
        return LoggerFactory.getLevel() >= TRACE_LEVEL;
    }

    public void trace(String msg) {
        if (LoggerFactory.getLevel() >= TRACE_LEVEL) LoggerFactory.log(TRACE_LEVEL, tag, msg, null);
    }

    public boolean isDebugEnabled() {
        return LoggerFactory.getLevel() >= DEBUG_LEVEL;
    }

    public void debug(String msg) {
        if (LoggerFactory.getLevel() >= DEBUG_LEVEL) LoggerFactory.log(DEBUG_LEVEL, tag, msg, null);
    }

    public void warn(String msg) {
        if (LoggerFactory.getLevel() >= WARN_LEVEL) LoggerFactory.log(WARN_LEVEL, tag, msg, null);
    }

    public void warn(String msg, Throwable e) {
        if (LoggerFactory.getLevel() >= WARN_LEVEL) LoggerFactory.log(WARN_LEVEL, tag, msg, e);
    }

    public void error(String msg) {
        if (LoggerFactory.getLevel() >= WARN_LEVEL) LoggerFactory.log(ERROR_LEVEL, tag, msg, null);
    }

    public void error(String msg, Throwable e) {
        if (LoggerFactory.getLevel() >= WARN_LEVEL) LoggerFactory.log(ERROR_LEVEL, tag, msg, e);
    }

    public void traceNio(SelectionKey key, String tag, String action) {
        if (isTraceEnabled()) {
            NetCtx atta = (NetCtx)key.attachment();
            if (atta != null) {
                TlsContext tctx = atta.getTlsContext();
                if (tctx != null) {
                    trace(keyToString(key) + " " + tag + " " + action + ", state=" + tctx.getSslState() +
                            ", netBuf=" + tctx.getSslNetBuf() + ", appBuf=" + tctx.getSslAppBuf());
                } else {
                    trace(keyToString(key) + " " + tag + " " + action + ", (no attachment)");
                }
            } else {
                trace(keyToString(key) + " " + tag + " " + action + ", (no attachment)");
            }
        }
    }
}
