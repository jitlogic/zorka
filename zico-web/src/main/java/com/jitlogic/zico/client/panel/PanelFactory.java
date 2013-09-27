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
package com.jitlogic.zico.client.panel;


import com.google.inject.assistedinject.Assisted;
import com.jitlogic.zico.data.HostInfo;
import com.jitlogic.zico.data.TraceInfo;

import java.util.Map;

public interface PanelFactory {

    public TraceListPanel traceListPanel(HostInfo hostInfo);

    public TraceDetailPanel traceDetailPanel(TraceInfo traceInfo);

    public TraceTemplatePanel traceTemplatePanel(Map<String, String> tidMap);

    public TraceRecordSearchDialog traceRecordSearchDialog(TraceDetailPanel panel, TraceInfo trace);

    public MethodAttrsDialog methodAttrsDialog(Integer hostId, Long dataOffs, String path, @Assisted("minTime") Long minTime);
}
