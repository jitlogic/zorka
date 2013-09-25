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
package com.jitlogic.zico.data;


import com.google.gwt.editor.client.Editor;
import com.jitlogic.zico.data.TraceInfo;
import com.sencha.gxt.core.client.ValueProvider;
import com.sencha.gxt.data.shared.LabelProvider;
import com.sencha.gxt.data.shared.ModelKeyProvider;
import com.sencha.gxt.data.shared.PropertyAccess;

public interface TraceInfoProperties extends PropertyAccess<TraceInfo> {

    @Editor.Path("dataOffs")
    ModelKeyProvider<TraceInfo> key();

    @Editor.Path("description")
    LabelProvider<TraceInfo> nameLabel();

    ValueProvider<TraceInfo, Integer> hostId();

    ValueProvider<TraceInfo, Long> dataOffs();

    ValueProvider<TraceInfo, Integer> traceId();

    ValueProvider<TraceInfo, String> traceType();

    ValueProvider<TraceInfo, Integer> dataLen();

    ValueProvider<TraceInfo, Long> clock();

    ValueProvider<TraceInfo, Integer> methodFlags();

    ValueProvider<TraceInfo, Integer> traceFlags();

    ValueProvider<TraceInfo, Long> calls();

    ValueProvider<TraceInfo, Long> errors();

    ValueProvider<TraceInfo, Long> records();

    ValueProvider<TraceInfo, Long> executionTime();

    ValueProvider<TraceInfo, String> description();
}

