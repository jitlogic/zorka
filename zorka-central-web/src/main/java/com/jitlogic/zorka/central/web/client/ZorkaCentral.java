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
package com.jitlogic.zorka.central.web.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;


public class ZorkaCentral implements EntryPoint {

    TabLayoutPanel tabPanel;


    public void add(IsWidget w, String title) {
        tabPanel.add(w, title);
        tabPanel.selectTab(w);
    }

    public void remove(Widget w) {
        tabPanel.remove(w);
    }

    public void onModuleLoad() {
        tabPanel = new TabLayoutPanel(2.5, Style.Unit.EM);
        tabPanel.add(new HostListPanel(this), "Hosts");
        //tabPanel.add(new TraceListPanel(), "Traces1");
        //tabPanel.add(new TraceListPanel(), "Traces2");
        tabPanel.setWidth("100%");
        tabPanel.setHeight("100%");

        RootPanel.get("CentralConsole").add(tabPanel);
    }
}
