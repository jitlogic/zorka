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
import com.jitlogic.zico.client.ErrorHandler;
import com.jitlogic.zico.client.Resources;
import com.jitlogic.zico.client.ZicoShell;
import com.jitlogic.zico.client.api.TraceDataApi;
import com.jitlogic.zico.data.HostInfo;
import com.jitlogic.zico.data.HostInfoProperties;
import com.sencha.gxt.core.client.Style;
import com.sencha.gxt.data.shared.ListStore;
import com.sencha.gxt.widget.core.client.Dialog;
import com.sencha.gxt.widget.core.client.box.ConfirmMessageBox;
import com.sencha.gxt.widget.core.client.button.TextButton;
import com.sencha.gxt.widget.core.client.container.VerticalLayoutContainer;
import com.sencha.gxt.widget.core.client.event.CellClickEvent;
import com.sencha.gxt.widget.core.client.event.CellDoubleClickEvent;
import com.sencha.gxt.widget.core.client.event.HideEvent;
import com.sencha.gxt.widget.core.client.event.SelectEvent;
import com.sencha.gxt.widget.core.client.grid.ColumnConfig;
import com.sencha.gxt.widget.core.client.grid.ColumnModel;
import com.sencha.gxt.widget.core.client.grid.Grid;
import com.sencha.gxt.widget.core.client.grid.GridSelectionModel;
import com.sencha.gxt.widget.core.client.menu.Item;
import com.sencha.gxt.widget.core.client.menu.Menu;
import com.sencha.gxt.widget.core.client.menu.MenuItem;
import com.sencha.gxt.widget.core.client.menu.SeparatorMenuItem;
import com.sencha.gxt.widget.core.client.toolbar.SeparatorToolItem;
import com.sencha.gxt.widget.core.client.toolbar.ToolBar;
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

    private ErrorHandler errorHandler;

    private TextButton btnRefresh;
    private TextButton btnAddHost;
    private TextButton btnRemoveHost;
    private TextButton btnEditHost;
    private TextButton btnListTraces;
    private MenuItem mnuRefresh;
    private MenuItem mnuNewHost;
    private MenuItem mnuRemoveHost;
    private MenuItem mnuEditHost;
    private MenuItem mnuListTraces;

    private boolean selectionDependentControlsEnabled = true;


    @Inject
    public HostListPanel(Provider<ZicoShell> shell, TraceDataApi traceDataApi, PanelFactory panelFactory,
                         ErrorHandler errorHandler) {

        this.shell = shell;
        this.traceDataApi = traceDataApi;
        this.panelFactory = panelFactory;
        this.errorHandler = errorHandler;

        createToolbar();
        createHostListPanel();
        createContextMenu();

        enableSelectionDependentControls(false);
    }

    private void enableSelectionDependentControls(boolean enabled) {
        if (selectionDependentControlsEnabled != enabled) {
            btnRemoveHost.setEnabled(enabled);
            btnEditHost.setEnabled(enabled);
            btnListTraces.setEnabled(enabled);
            mnuRemoveHost.setEnabled(enabled);
            mnuEditHost.setEnabled(enabled);
            mnuListTraces.setEnabled(enabled);
            selectionDependentControlsEnabled = enabled;
        }
    }

    private void createToolbar() {
        ToolBar toolBar = new ToolBar();

        btnRefresh = new TextButton();
        btnRefresh.setIcon(Resources.INSTANCE.refreshIcon());
        btnRefresh.setToolTip("Refresh host list");
        btnRefresh.addSelectHandler(new SelectEvent.SelectHandler() {
            @Override
            public void onSelect(SelectEvent event) {
                refresh();
            }
        });
        toolBar.add(btnRefresh);

        toolBar.add(new SeparatorToolItem());

        btnAddHost = new TextButton();
        btnAddHost.setIcon(Resources.INSTANCE.addIcon());
        btnAddHost.setToolTip("Add new host");
        btnAddHost.addSelectHandler(new SelectEvent.SelectHandler() {
            @Override
            public void onSelect(SelectEvent event) {
                addHost();
            }
        });
        toolBar.add(btnAddHost);

        btnRemoveHost = new TextButton();
        btnRemoveHost.setIcon(Resources.INSTANCE.removeIcon());
        btnRemoveHost.setToolTip("Remove host");
        btnRemoveHost.addSelectHandler(new SelectEvent.SelectHandler() {
            @Override
            public void onSelect(SelectEvent event) {
                removeHost();
            }
        });
        toolBar.add(btnRemoveHost);

        btnEditHost = new TextButton();
        btnEditHost.setIcon(Resources.INSTANCE.editIcon());
        btnEditHost.setToolTip("Edit host");
        toolBar.add(btnEditHost);

        toolBar.add(new SeparatorToolItem());

        btnListTraces = new TextButton();
        btnListTraces.setIcon(Resources.INSTANCE.listColumnsIcon());
        btnListTraces.setToolTip("List traces");
        toolBar.add(btnListTraces);

        btnListTraces.addSelectHandler(new SelectEvent.SelectHandler() {
            @Override
            public void onSelect(SelectEvent event) {
                listTraces();
            }
        });

        add(toolBar, new VerticalLayoutData(1, -1));
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

        hostGrid.getView().setAutoExpandColumn(addrCol);
        hostGrid.getView().setForceFit(true);

        // TODO host selection handler for keyboard: select item, press ENTER

        refresh();

        hostGrid.addCellDoubleClickHandler(new CellDoubleClickEvent.CellDoubleClickHandler() {
            @Override
            public void onCellClick(CellDoubleClickEvent event) {
                enableSelectionDependentControls(hostGrid.getSelectionModel().getSelectedItem() != null);
                listTraces();
            }
        });

        hostGrid.addCellClickHandler(new CellClickEvent.CellClickHandler() {
            @Override
            public void onCellClick(CellClickEvent event) {
                enableSelectionDependentControls(hostGrid.getSelectionModel().getSelectedItem() != null);
            }
        });

        add(hostGrid, new VerticalLayoutData(1, 1));
    }


    public void refresh() {
        hostGridStore.clear();
        this.traceDataApi.listHosts(new MethodCallback<List<HostInfo>>() {
            @Override
            public void onFailure(Method method, Throwable exception) {
                errorHandler.error("Error calling API method: " + method, exception);
            }

            @Override
            public void onSuccess(Method method, List<HostInfo> response) {
                hostGridStore.addAll(response);
                enableSelectionDependentControls(hostGrid.getSelectionModel().getSelectedItem() != null);
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
                                errorHandler.error("Error calling API method: " + method, exception);
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

        mnuRefresh = new MenuItem("Refresh");
        mnuRefresh.setIcon(Resources.INSTANCE.refreshIcon());
        mnuRefresh.addSelectionHandler(new SelectionHandler<Item>() {
            @Override
            public void onSelection(SelectionEvent<Item> event) {
                refresh();
            }
        });
        menu.add(mnuRefresh);

        menu.add(new SeparatorMenuItem());

        mnuNewHost = new MenuItem("New host");
        mnuNewHost.setIcon(Resources.INSTANCE.addIcon());
        mnuNewHost.addSelectionHandler(new SelectionHandler<Item>() {
            @Override
            public void onSelection(SelectionEvent<Item> event) {
                addHost();
            }
        });
        menu.add(mnuNewHost);

        mnuRemoveHost = new MenuItem("Remove host");
        mnuRemoveHost.setIcon(Resources.INSTANCE.removeIcon());
        mnuRemoveHost.addSelectionHandler(new SelectionHandler<Item>() {
            @Override
            public void onSelection(SelectionEvent<Item> event) {
                removeHost();
            }
        });
        menu.add(mnuRemoveHost);

        mnuEditHost = new MenuItem("Edit host");
        mnuEditHost.setIcon(Resources.INSTANCE.editIcon());
        mnuEditHost.addSelectionHandler(new SelectionHandler<Item>() {
            @Override
            public void onSelection(SelectionEvent<Item> event) {
                editHost();
            }
        });
        menu.add(mnuEditHost);

        menu.add(new SeparatorMenuItem());

        mnuListTraces = new MenuItem("List traces");
        mnuListTraces.setIcon(Resources.INSTANCE.listColumnsIcon());
        mnuListTraces.addSelectionHandler(new SelectionHandler<Item>() {
            @Override
            public void onSelection(SelectionEvent<Item> event) {
                listTraces();
            }
        });
        menu.add(mnuListTraces);

        hostGrid.setContextMenu(menu);
    }


    private void addHost() {
        new HostPrefsDialog(traceDataApi, this, null, errorHandler).show();
    }


    private void removeHost() {
        removeHost(hostGrid.getSelectionModel().getSelectedItem());
    }


    private void editHost() {
        HostInfo hostInfo = hostGrid.getSelectionModel().getSelectedItem();
        if (hostInfo != null) {
            new HostPrefsDialog(traceDataApi, this, hostInfo, errorHandler).show();
        }
    }


    private void listTraces() {
        HostInfo hostInfo = selectionModel.getSelectedItem();
        GWT.log("Selected host: " + hostInfo);

        if (hostInfo != null) {
            shell.get().addView(panelFactory.traceListPanel(hostInfo), hostInfo.getName() + ": traces");
        }
    }
}
