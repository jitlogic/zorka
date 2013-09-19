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

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.inject.Inject;
import com.jitlogic.zico.client.Resources;
import com.jitlogic.zico.client.ZicoShell;
import com.jitlogic.zico.client.ZicoShell;
import com.jitlogic.zico.client.api.TraceDataApi;
import com.jitlogic.zico.data.HostInfo;
import com.jitlogic.zico.data.HostInfoProperties;
import com.sencha.gxt.core.client.Style;
import com.sencha.gxt.data.shared.ListStore;
import com.sencha.gxt.widget.core.client.Dialog;
import com.sencha.gxt.widget.core.client.box.AlertMessageBox;
import com.sencha.gxt.widget.core.client.box.ConfirmMessageBox;
import com.sencha.gxt.widget.core.client.container.VerticalLayoutContainer;
import com.sencha.gxt.widget.core.client.event.CellDoubleClickEvent;
import com.sencha.gxt.widget.core.client.event.HideEvent;
import com.sencha.gxt.widget.core.client.grid.ColumnConfig;
import com.sencha.gxt.widget.core.client.grid.ColumnModel;
import com.sencha.gxt.widget.core.client.grid.Grid;
import com.sencha.gxt.widget.core.client.grid.GridSelectionModel;
import com.sencha.gxt.widget.core.client.menu.Item;
import com.sencha.gxt.widget.core.client.menu.Menu;
import com.sencha.gxt.widget.core.client.menu.MenuItem;
import com.sencha.gxt.widget.core.client.menu.SeparatorMenuItem;
import org.fusesource.restygwt.client.Method;
import org.fusesource.restygwt.client.MethodCallback;

import javax.inject.Provider;
import java.util.Arrays;
import java.util.List;


public class HostListPanel extends VerticalLayoutContainer {

    private static final HostInfoProperties props = GWT.create(HostInfoProperties.class);

    private TraceDataApi traceDataApi;
    private Provider<ZicoShell> shell;
    private PanelFactory panelFactory;

    private Grid<HostInfo> hostGrid;
    private ListStore<HostInfo> hostGridStore;
    private GridSelectionModel<HostInfo> selectionModel;


    @Inject
    public HostListPanel(Provider<ZicoShell> shell, TraceDataApi traceDataApi, PanelFactory panelFactory) {
        this.shell = shell;
        this.traceDataApi = traceDataApi;
        this.panelFactory = panelFactory;

        createHostListPanel();
        createContextMenu();
    }


    private void createHostListPanel() {
        ColumnConfig<HostInfo, String> nameCol = new ColumnConfig<HostInfo, String>(props.name(), 128, "Host Name");
        nameCol.setMenuDisabled(true);
        nameCol.setSortable(false);

        ColumnConfig<HostInfo, String> addrCol = new ColumnConfig<HostInfo, String>(props.addr(), 127, "IP Address");
        addrCol.setMenuDisabled(true);
        addrCol.setSortable(false);

        ColumnModel<HostInfo> model = new ColumnModel<HostInfo>(Arrays.<ColumnConfig<HostInfo, ?>>asList(nameCol, addrCol));
        hostGridStore = new ListStore<HostInfo>(props.key());

        hostGrid = new Grid<HostInfo>(hostGridStore, model);
        selectionModel = hostGrid.getSelectionModel();
        selectionModel.setSelectionMode(Style.SelectionMode.SINGLE);

        // TODO host selection handler for keyboard: select item, press ENTER

        refresh();

        hostGrid.addCellDoubleClickHandler(new CellDoubleClickEvent.CellDoubleClickHandler() {
            @Override
            public void onCellClick(CellDoubleClickEvent event) {
                selectHost();
            }
        });

        add(hostGrid, new VerticalLayoutData(1, 1));
    }


    public void refresh() {
        hostGridStore.clear();
        this.traceDataApi.listHosts(new MethodCallback<List<HostInfo>>() {
            @Override
            public void onFailure(Method method, Throwable exception) {
                GWT.log("Error calling " + method, exception);
            }

            @Override
            public void onSuccess(Method method, List<HostInfo> response) {
                hostGridStore.addAll(response);
            }
        });
    }


    private void removeHost(final HostInfo hi) {
        if (hi != null) {
            ConfirmMessageBox cmb = new ConfirmMessageBox(
                    "Removing host", "Are you sure you want to remove host " + hi.getName() + "?");
            cmb.addHideHandler(new HideEvent.HideHandler() {
                @Override
                public void onHide(HideEvent event) {
                    Dialog d = (Dialog) event.getSource();
                    if ("Yes".equals(d.getHideButton().getText())) {
                        traceDataApi.deleteHost(hi.getId(), new MethodCallback<Void>() {
                            @Override
                            public void onFailure(Method method, Throwable exception) {
                                AlertMessageBox amb = new AlertMessageBox("Error saving host", exception.getMessage());
                                amb.show();
                            }

                            @Override
                            public void onSuccess(Method method, Void response) {
                                refresh();
                            }
                        });
                    }
                }
            });
            cmb.show();
        }
    }


    private void createContextMenu() {
        Menu menu = new Menu();

        MenuItem mnuRefresh = new MenuItem("Refresh");
        mnuRefresh.setIcon(Resources.INSTANCE.refreshIcon());
        menu.add(mnuRefresh);

        mnuRefresh.addSelectionHandler(new SelectionHandler<Item>() {
            @Override
            public void onSelection(SelectionEvent<Item> event) {
                refresh();
            }
        });

        menu.add(new SeparatorMenuItem());

        MenuItem mnuNewHost = new MenuItem("New host");
        mnuNewHost.setIcon(Resources.INSTANCE.addIcon());
        menu.add(mnuNewHost);

        mnuNewHost.addSelectionHandler(new SelectionHandler<Item>() {
            @Override
            public void onSelection(SelectionEvent<Item> event) {
                HostPrefsDialog dialog = new HostPrefsDialog(traceDataApi, HostListPanel.this, null);
                dialog.show();
            }
        });

        MenuItem mnuRemoveHost = new MenuItem("Remove host");
        mnuRemoveHost.setIcon(Resources.INSTANCE.removeIcon());
        menu.add(mnuRemoveHost);

        mnuRemoveHost.addSelectionHandler(new SelectionHandler<Item>() {
            @Override
            public void onSelection(SelectionEvent<Item> event) {
                removeHost(hostGrid.getSelectionModel().getSelectedItem());
            }
        });

        MenuItem mnuEditHost = new MenuItem("Edit host");
        mnuEditHost.setIcon(Resources.INSTANCE.editIcon());
        menu.add(mnuEditHost);

        mnuEditHost.addSelectionHandler(new SelectionHandler<Item>() {
            @Override
            public void onSelection(SelectionEvent<Item> event) {
                HostInfo info = hostGrid.getSelectionModel().getSelectedItem();
                if (info != null) {
                    HostPrefsDialog dialog = new HostPrefsDialog(traceDataApi, HostListPanel.this, info);
                    dialog.show();
                }
            }
        });

        menu.add(new SeparatorMenuItem());

        MenuItem mnuListTraces = new MenuItem("List traces");
        mnuListTraces.setIcon(Resources.INSTANCE.listColumnsIcon());
        menu.add(mnuListTraces);

        mnuListTraces.addSelectionHandler(new SelectionHandler<Item>() {
            @Override
            public void onSelection(SelectionEvent<Item> event) {
                selectHost();
            }
        });

        hostGrid.setContextMenu(menu);
    }

    private void selectHost() {

        HostInfo hostInfo = selectionModel.getSelectedItem();
        GWT.log("Selected host: " + hostInfo);

        shell.get().addView(panelFactory.traceListPanel(hostInfo), hostInfo.getName() + ": traces");
    }
}
