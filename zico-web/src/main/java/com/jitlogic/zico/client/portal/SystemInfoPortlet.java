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
package com.jitlogic.zico.client.portal;


import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Label;
import com.google.inject.Inject;
import com.jitlogic.zico.client.api.SystemApi;
import com.sencha.gxt.widget.core.client.Portlet;
import com.sencha.gxt.widget.core.client.container.VerticalLayoutContainer;
import org.fusesource.restygwt.client.Method;
import org.fusesource.restygwt.client.MethodCallback;

import java.util.List;

public class SystemInfoPortlet extends Portlet {

    VerticalLayoutContainer content;

    private SystemApi systemApi;
    private Timer timer;

    @Inject
    public SystemInfoPortlet(SystemApi systemApi) {
        this.systemApi = systemApi;

        setHeadingText("System info");
        setCollapsible(true);

        content = new VerticalLayoutContainer();
        content.add(new Label("Wait..."));
        add(content);

//        getHeader().addTool(new ToolButton(ToolButton.CLOSE, new SelectEvent.SelectHandler() {
//            @Override
//            public void onSelect(SelectEvent event) {
//                if (timer != null) {
//                    timer.cancel();
//                }
//                SystemInfoPortlet.this.removeFromParent();
//            }
//        }));

        loadData();

        timer = new Timer() {
            @Override
            public void run() {
                loadData();
            }
        };
        timer.scheduleRepeating(10000);
    }

    private void loadData() {
        systemApi.systemInfo(new MethodCallback<List<String>>() {
            @Override
            public void onFailure(Method method, Throwable exception) {
                content.clear();
                content.add(new Label("Error: " + exception));
            }

            @Override
            public void onSuccess(Method method, List<String> response) {
                content.clear();
                for (String s : response) {
                    content.add(new Label(s));
                }
            }
        });
    }

}
