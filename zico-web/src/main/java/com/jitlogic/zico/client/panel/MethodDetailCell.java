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


import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.jitlogic.zico.shared.data.KeyValueProxy;
import com.jitlogic.zico.shared.data.SymbolicExceptionProxy;
import com.jitlogic.zico.shared.data.TraceRecordProxy;

public class MethodDetailCell extends AbstractCell<TraceRecordProxy> {

    @Override
    public void render(Context context, TraceRecordProxy tr, SafeHtmlBuilder sb) {
        if (tr.getAttributes() != null) {
            sb.appendHtmlConstant("<table border=\"0\" cellspacing=\"2\"><tbody>");
            for (KeyValueProxy e : tr.getAttributes()) {
                sb.appendHtmlConstant("<tr><td align=\"right\" style=\"color:blue; font-size: small;\"><b>");
                sb.append(SafeHtmlUtils.fromString(e.getKey()));
                sb.appendHtmlConstant("</b></td><td><div style=\"text-wrap: unrestricted; white-space: pre; word-wrap: break-word; font-size: small;\">");
                sb.append(SafeHtmlUtils.fromString(e.getValue() != null ? e.getValue().toString() : ""));
                sb.appendHtmlConstant("</div></td></tr>");
            }
            sb.appendHtmlConstant("</tbody></table>");
        }
        if (tr.getExceptionInfo() != null) {
            SymbolicExceptionProxy e = tr.getExceptionInfo();
            sb.appendHtmlConstant("<div><span style=\"color: red;\">");
            sb.append(SafeHtmlUtils.fromString("Caught: " + e.getExClass()));
            sb.appendHtmlConstant("</span></div><div><b>");
            sb.append(SafeHtmlUtils.fromString("" + e.getMessage()));
            sb.appendHtmlConstant("</b></div>");
            int i = 0;
            for (String s : e.getStackTrace()) {
                sb.appendHtmlConstant("<div>");
                sb.append(SafeHtmlUtils.fromString("" + s));
                sb.appendHtmlConstant("</div>");
                i++;
                if (i > 5) {
                    sb.appendHtmlConstant("<div>");
                    sb.append(SafeHtmlUtils.fromString("...      (open method attributes window to see full stack trace)"));
                    sb.appendHtmlConstant("</div>");
                    break;
                }
            }
        }
    }
}
