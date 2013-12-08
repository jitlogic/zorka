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
import com.google.web.bindery.requestfactory.shared.Receiver;
import com.google.web.bindery.requestfactory.shared.ServerFailure;
import com.jitlogic.zico.client.inject.ZicoRequestFactory;
import com.sencha.gxt.widget.core.client.Portlet;
import com.sencha.gxt.widget.core.client.container.VerticalLayoutContainer;

import java.util.List;

public class SystemInfoPortlet extends Portlet {

    VerticalLayoutContainer content;

    private ZicoRequestFactory rf;
    private Timer timer;

    @Inject
    public SystemInfoPortlet(ZicoRequestFactory rf) {
        this.rf = rf;

        setHeadingText("System info");
        setCollapsible(true);

        content = new VerticalLayoutContainer();
        content.add(new Label("Wait..."));
        add(content);

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
        rf.systemService().systemInfo().fire(new Receiver<List<String>>() {
            @Override
            public void onSuccess(List<String> response) {
                content.clear();
                for (String s : response) {
                    content.add(new Label(s));
                }
            }

            @Override
            public void onFailure(ServerFailure e) {
                content.clear();
                content.add(new Label("Error: " + e.getMessage()));

            }
        });
    }

}
