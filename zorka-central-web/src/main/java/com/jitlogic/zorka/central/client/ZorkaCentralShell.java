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

import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Widget;
import com.sencha.gxt.core.client.util.Margins;
import com.sencha.gxt.widget.core.client.ContentPanel;
import com.sencha.gxt.widget.core.client.TabItemConfig;
import com.sencha.gxt.widget.core.client.TabPanel;
import com.sencha.gxt.widget.core.client.container.BorderLayoutContainer;
import com.sencha.gxt.widget.core.client.container.MarginData;
import com.sencha.gxt.widget.core.client.container.SimpleContainer;

public class ZorkaCentralShell extends BorderLayoutContainer {

    private TabPanel tabPanel;

    private TraceAdminService adminService;

    private final static int DX = 0;
    private final static int DY = 0;

    public ZorkaCentralShell(TraceDataService tds, TraceAdminService adminService) {

        Window.enableScrolling(false);
        setPixelSize(Window.getClientWidth() - DX, Window.getClientHeight() - DY);

        Window.addResizeHandler(new ResizeHandler() {
            @Override
            public void onResize(ResizeEvent event) {
                ZorkaCentralShell.this.setPixelSize(Window.getClientWidth() - DX, Window.getClientHeight() - DY);
            }
        });


        BorderLayoutContainer.BorderLayoutData westData = new BorderLayoutContainer.BorderLayoutData(256);
        westData.setMargins(new Margins(5, 0, 5, 5));
        westData.setSplit(true);
        westData.setCollapsible(true);
        westData.setCollapseHidden(true);
        westData.setCollapseMini(true);

        ContentPanel westContainer = new ContentPanel();
        westContainer.setHeadingText("Hosts");
        westContainer.setBodyBorder(true);
        westContainer.add(new HostListPanel(this, tds));

        setWestWidget(westContainer, westData);

        tabPanel = new TabPanel();
        tabPanel.setBodyBorder(true);
        tabPanel.setTabScroll(true);
        tabPanel.setCloseContextMenu(true);

        tabPanel.setCloseContextMenu(true);

        this.adminService = adminService;

        MarginData centerData = new MarginData();
        centerData.setMargins(new Margins(5));

        SimpleContainer center = new SimpleContainer();
        center.add(tabPanel);

        setCenterWidget(center, centerData);

        tabPanel.add(new WelcomePanel(this), new TabItemConfig("Welcome"));
    }

    public TraceAdminService getAdminService() {
        return adminService;
    }

    public void addView(Widget widget, String title) {
        TabItemConfig tic = new TabItemConfig(title);
        tic.setClosable(true);
        tabPanel.add(widget, tic);
        tabPanel.setActiveWidget(widget);
    }


    @Override
    protected void onWindowResize(int width, int height) {
        setPixelSize(width - DX, height - DY);
    }

}
