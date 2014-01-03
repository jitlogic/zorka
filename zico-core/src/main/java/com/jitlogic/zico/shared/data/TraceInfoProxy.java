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
package com.jitlogic.zico.shared.data;

import com.google.web.bindery.requestfactory.shared.ProxyFor;
import com.google.web.bindery.requestfactory.shared.ValueProxy;
import com.jitlogic.zico.core.model.TraceInfo;

import java.util.List;

@ProxyFor(TraceInfo.class)
public interface TraceInfoProxy extends ValueProxy {
    String getHostName();

    long getDataOffs();

    int getTraceId();

    String getTraceType();

    int getDataLen();

    long getClock();

    int getMethodFlags();

    int getTraceFlags();

    int getStatus();

    int getClassId();

    int getMethodId();

    int getSignatureId();

    long getCalls();

    long getErrors();

    long getRecords();

    long getExecutionTime();

    String getDescription();

    List<KeyValueProxy> getAttributes();

    SymbolicExceptionProxy getExceptionInfo();
}
