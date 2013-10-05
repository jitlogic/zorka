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
import com.jitlogic.zico.client.ErrorHandler;
import com.jitlogic.zico.client.api.TraceDataApi;
import com.jitlogic.zico.data.SymbolicExceptionInfo;
import com.jitlogic.zico.data.TraceInfo;
import com.jitlogic.zico.data.TraceRecordInfo;
import com.sencha.gxt.core.client.util.Margins;
import com.sencha.gxt.widget.core.client.Dialog;
import com.sencha.gxt.widget.core.client.container.VerticalLayoutContainer;
import com.sencha.gxt.widget.core.client.form.TextArea;
import org.fusesource.restygwt.client.Method;
import org.fusesource.restygwt.client.MethodCallback;

import javax.inject.Inject;
import java.util.Map;

public class MethodAttrsDialog extends Dialog {

    private TextArea txtAttrs;
    private TextArea txtException;

    private TraceDataApi api;
    private ErrorHandler errorHandler;

    @Inject
    public MethodAttrsDialog(TraceDataApi api, ErrorHandler errorHandler,
                             @Assisted Integer hostId, @Assisted Long dataOffs,
                             @Assisted String path, @Assisted("minTime") Long minTime) {
        this.api = api;
        this.errorHandler = errorHandler;
        configure("Trace Details");

        txtAttrs.setText("Please wait ...");

        loadTraceDetail(hostId, dataOffs, path, minTime);
    }


    private void loadTraceDetail(Integer hostId, Long dataOffs, String path, Long minTime) {
        api.getTraceRecord(hostId, dataOffs, minTime, path,
                new MethodCallback<TraceRecordInfo>() {
                    @Override
                    public void onFailure(Method method, Throwable exception) {
                        errorHandler.error("Error calling method: " + method, exception);
                    }

                    @Override
                    public void onSuccess(Method method, TraceRecordInfo tr) {
                        if (tr.getAttributes() != null) {
                            fillAttrs(tr.getAttributes());
                        }

                        if (tr.getExceptionInfo() != null) {
                            fillExceptionInfo(tr.getExceptionInfo());
                        }
                    }
                });
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
