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


import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Widget;
import com.jitlogic.zorka.central.data.HostInfo;
import com.jitlogic.zorka.central.data.HostInfoProperties;
import com.sencha.gxt.core.client.Style;
import com.sencha.gxt.core.client.util.Margins;
import com.sencha.gxt.data.shared.ListStore;
import com.sencha.gxt.widget.core.client.ContentPanel;
import com.sencha.gxt.widget.core.client.TabItemConfig;
import com.sencha.gxt.widget.core.client.TabPanel;
import com.sencha.gxt.widget.core.client.container.BorderLayoutContainer;
import com.sencha.gxt.widget.core.client.container.HtmlLayoutContainer;
import com.sencha.gxt.widget.core.client.container.MarginData;
import com.sencha.gxt.widget.core.client.event.CellDoubleClickEvent;
import com.sencha.gxt.widget.core.client.grid.ColumnConfig;
import com.sencha.gxt.widget.core.client.grid.ColumnModel;
import com.sencha.gxt.widget.core.client.grid.Grid;
import com.sencha.gxt.widget.core.client.grid.GridSelectionModel;
import org.fusesource.restygwt.client.Method;
import org.fusesource.restygwt.client.MethodCallback;

import java.util.Arrays;
import java.util.List;

public class ZorkaCentralShell extends BorderLayoutContainer {

    private static final HostInfoProperties props = GWT.create(HostInfoProperties.class);

    private TraceDataService tds;
    private Grid<HostInfo> hostGrid;

    private TabPanel tabPanel;

    //private BorderLayoutStateHandler state;

    public ZorkaCentralShell(TraceDataService tds) {

        this.tds = tds;

        //shell.setStateful(true);
        //shell.setStateId("test");

        //state = new BorderLayoutStateHandler(shell);
        //state.loadState();

        setPixelSize(Window.getClientWidth() - 10, Window.getClientHeight() - 16);

        //HtmlLayoutContainer northPanel = new HtmlLayoutContainer("<div>NORTH</div>");
        //BorderLayoutData northData = new BorderLayoutContainer.BorderLayoutData(35);
        //setNorthWidget(northPanel, northData);

        createHostListPanel();

        //traceListPanel.setPixelSize(1200, 800);

        tabPanel = new TabPanel();
        tabPanel.setBodyBorder(true);
        tabPanel.setTabScroll(true);
        tabPanel.setCloseContextMenu(true);


        //HtmlLayoutContainer centerPanel = new HtmlLayoutContainer("<div>CENTER</div>");
        MarginData centerData = new MarginData();
        centerData.setMargins(new Margins(5));
        setCenterWidget(tabPanel, centerData);

    }

    private void createHostListPanel() {
        ColumnConfig<HostInfo, String> nameCol = new ColumnConfig<HostInfo, String>(props.name(), 128, "Host Name");
        ColumnConfig<HostInfo, String> addrCol = new ColumnConfig<HostInfo, String>(props.addr(), 127, "IP Address");
        ColumnModel<HostInfo> model = new ColumnModel<HostInfo>(Arrays.<ColumnConfig<HostInfo, ?>>asList(nameCol, addrCol));
        final ListStore<HostInfo> store = new ListStore<HostInfo>(props.key());

        hostGrid = new Grid<HostInfo>(store, model);
        final GridSelectionModel<HostInfo> selectionModel = hostGrid.getSelectionModel();
        selectionModel.setSelectionMode(Style.SelectionMode.SINGLE);

        // TODO host selection handler for keyboard: select item, press ENTER

        this.tds.listHosts(new MethodCallback<List<HostInfo>>() {
            @Override
            public void onFailure(Method method, Throwable exception) {
                GWT.log("Error calling " + method, exception);
            }

            @Override
            public void onSuccess(Method method, List<HostInfo> response) {
                store.addAll(response);
            }
        });

        hostGrid.addCellDoubleClickHandler(new CellDoubleClickEvent.CellDoubleClickHandler() {
            @Override
            public void onCellClick(CellDoubleClickEvent event) {
                GWT.log("Selected host: " + selectionModel.getSelectedItem());
                addView(new TraceListingPanel(ZorkaCentralShell.this, tds, selectionModel.getSelectedItem()),
                        selectionModel.getSelectedItem().getName() + ": traces");
            }
        });

        ContentPanel westContainer = new ContentPanel();
        westContainer.setHeadingText("Hosts");
        westContainer.setBodyBorder(true);
        westContainer.add(hostGrid);

        BorderLayoutData westData = new BorderLayoutData(256);
        westData.setMargins(new Margins(5, 0, 5, 5));
        westData.setSplit(true);
        westData.setCollapsible(true);
        westData.setCollapseHidden(true);
        westData.setCollapseMini(true);
        setWestWidget(westContainer, westData);
    }


    public void addView(Widget widget, String title) {
        TabItemConfig tic = new TabItemConfig(title);
        tic.setClosable(true);
        tabPanel.add(widget, tic);
        tabPanel.setActiveWidget(widget);
    }


    @Override
    protected void onWindowResize(int width, int height) {
        setPixelSize(width - 10, height - 16);
    }

}
