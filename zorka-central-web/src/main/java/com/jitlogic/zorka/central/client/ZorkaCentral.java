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

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.*;
import com.jitlogic.zorka.central.client.data.TraceDataService;
import org.fusesource.restygwt.client.Resource;
import org.fusesource.restygwt.client.RestServiceProxy;


public class ZorkaCentral implements EntryPoint {

    private TabLayoutPanel tabPanel;

    private TraceDataService traceDataService;

    public void add(IsWidget w, String title) {
        tabPanel.add(w, title);
        tabPanel.selectTab(w);
    }

    public void remove(Widget w) {
        tabPanel.remove(w);
    }

    public TraceDataService getTraceDataService() {
        return traceDataService;
    }

    public void onModuleLoad() {

        traceDataService = GWT.create(TraceDataService.class);
        ((RestServiceProxy) traceDataService).setResource(new Resource(GWT.getHostPageBaseURL() + "roof"));

        tabPanel = new TabLayoutPanel(2.5, Style.Unit.EM);
        tabPanel.add(new HostListPanel(this), "Hosts");
        //tabPanel.add(new TraceListPanel(), "Traces1");
        //tabPanel.add(new TraceListPanel(), "Traces2");
        tabPanel.setWidth("100%");
        tabPanel.setHeight("100%");

        RootPanel.get("CentralConsole").add(tabPanel);
    }
}
