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
package com.jitlogic.zorka.central.client;


import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.jitlogic.zorka.central.data.SymbolicExceptionInfo;
import com.jitlogic.zorka.central.data.TraceInfo;

import java.util.Map;

public class TraceDetailCell extends AbstractCell<TraceInfo> {

    @Override
    public void render(Context context, TraceInfo ti, SafeHtmlBuilder sb) {
        if (ti.getAttributes() != null) {
            for (Map.Entry<String, String> e : ti.getAttributes().entrySet()) {
                sb.appendHtmlConstant("<div style=\"white-space: nowrap;\">");
                sb.appendHtmlConstant("<span style=\"color: blue;\">");
                sb.append(SafeHtmlUtils.fromString(e.getKey() + " = "));
                sb.appendHtmlConstant("</span>");
                sb.append(SafeHtmlUtils.fromString("" + e.getValue()));
                sb.appendHtmlConstant("</div>");
            }
        }
        if (ti.getExceptionInfo() != null) {
            SymbolicExceptionInfo e = ti.getExceptionInfo();
            sb.appendHtmlConstant("<div><span style=\"color: red;\">");
            sb.append(SafeHtmlUtils.fromString("Caught: " + e.getExClass()));
            sb.appendHtmlConstant("</span></div><div><b>");
            sb.append(SafeHtmlUtils.fromString(e.getMessage()));
            sb.appendHtmlConstant("</b></div>");
            for (String s : e.getStackTrace()) {
                sb.appendHtmlConstant("<div>");
                sb.append(SafeHtmlUtils.fromString(s));
                sb.appendHtmlConstant("</div>");
            }
        }
    }
}
