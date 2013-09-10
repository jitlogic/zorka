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


import com.jitlogic.zorka.central.data.SymbolicExceptionInfo;
import com.jitlogic.zorka.central.data.TraceInfo;
import com.jitlogic.zorka.central.data.TraceRecordInfo;
import com.sencha.gxt.core.client.util.Margins;
import com.sencha.gxt.widget.core.client.Dialog;
import com.sencha.gxt.widget.core.client.container.VerticalLayoutContainer;
import com.sencha.gxt.widget.core.client.form.TextArea;

import java.util.Map;

public class MethodAttrsDialog extends Dialog {

    private TextArea txtAttrs;
    private TextArea txtException;

    public MethodAttrsDialog(TraceInfo ti) {
        configure("Trace Details");

        if (ti.getAttributes() != null) {
            fillAttrs(ti.getAttributes());
        }

        if (ti.getExceptionInfo() != null) {
            fillExceptionInfo(ti.getExceptionInfo());
        }
    }

    public MethodAttrsDialog(TraceRecordInfo tr) {
        configure("Method Details");

        if (tr.getAttributes() != null) {
            fillAttrs(tr.getAttributes());
        }

        if (tr.getExceptionInfo() != null) {
            fillExceptionInfo(tr.getExceptionInfo());
        }
    }

    private void fillAttrs(Map<String, String> attrs) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : attrs.entrySet()) {
            sb.append(e.getKey() + "=" + e.getValue() + "\n");
        }
        txtAttrs.setText(sb.toString());
    }

    private void fillExceptionInfo(SymbolicExceptionInfo e) {
        StringBuilder sb = new StringBuilder();
        sb.append(e.getExClass() + ": " + e.getMessage() + "\n");
        for (String s : e.getStackTrace()) {
            sb.append(s + "\n");
        }
        txtException.setText(sb.toString());
    }

    private void configure(String headingText) {
        setHeadingText(headingText);
        setPredefinedButtons();
        setPixelSize(1200, 850);

        VerticalLayoutContainer vp = new VerticalLayoutContainer();
        VerticalLayoutContainer.VerticalLayoutData vd = new VerticalLayoutContainer.VerticalLayoutData();
        vd.setMargins(new Margins(10, 0, 0, 0));
        vp.setLayoutData(vd);

        txtAttrs = new TextArea();
        txtAttrs.setPixelSize(1190, 400);
        txtAttrs.setReadOnly(true);
        vp.add(txtAttrs);

        txtException = new TextArea();
        txtException.setPixelSize(1195, 405);
        txtException.setReadOnly(true);
        vp.add(txtException);

        add(vp);
    }
}
