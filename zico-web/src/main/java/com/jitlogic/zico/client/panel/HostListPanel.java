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

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.inject.Inject;
import com.google.web.bindery.requestfactory.shared.Receiver;
import com.jitlogic.zico.client.ErrorHandler;
import com.jitlogic.zico.client.Resources;
import com.jitlogic.zico.client.ZicoShell;
import com.jitlogic.zico.client.inject.PanelFactory;
import com.jitlogic.zico.client.inject.ZicoRequestFactory;
import com.jitlogic.zico.client.props.HostInfoProperties;
import com.jitlogic.zico.shared.data.HostProxy;
import com.jitlogic.zico.shared.services.HostServiceProxy;
import com.sencha.gxt.core.client.IdentityValueProvider;
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

import javax.inject.Provider;
import java.util.Arrays;
import java.util.List;


public class HostListPanel extends VerticalLayoutContainer {

    private static final HostInfoProperties props = GWT.create(HostInfoProperties.class);

    private Provider<ZicoShell> shell;
    private PanelFactory panelFactory;
    private ZicoRequestFactory rf;

    private Grid<HostProxy> hostGrid;
    private ListStore<HostProxy> hostGridStore;
    private GridSelectionModel<HostProxy> selectionModel;

    private ErrorHandler errorHandler;

    private TextButton btnRefresh, btnAddHost, btnRemoveHost, btnEditHost, btnListTraces, btnDisableHost, btnEnableHost;
    private MenuItem mnuRefresh, mnuAddHost, mnuRemoveHost, mnuEditHost, mnuListTraces, mnuDisableHost, mnuEnableHost;

    private MenuItem mnuRebuildIndex, mnuOfflineHost, mnuOnlineHost;

    private boolean selectionDependentControlsEnabled = true;

    // TODO this is very badly written crutch. It will be fixed as soon as we move to GWT 2.6, so it will be in sync with Jetty 9.
    private boolean adminMode = false;
    private Menu contextMenu;

    @Inject
    public HostListPanel(Provider<ZicoShell> shell, PanelFactory panelFactory,
                         ZicoRequestFactory rf, ErrorHandler errorHandler) {

        this.shell = shell;
        this.panelFactory = panelFactory;
        this.rf = rf;
        this.errorHandler = errorHandler;

        createToolbar();
        createHostListPanel();
        createContextMenu();

        enableSelectionDependentControls(null);
    }

    public void setAdminMode(boolean adminMode) {
        this.adminMode = adminMode;
        enableSelectionDependentControls(null);

        if (adminMode) {
            if (mnuRebuildIndex == null) {
                contextMenu.add(new SeparatorMenuItem());
                mnuRebuildIndex = new MenuItem("Rebuild Index");
                contextMenu.add(mnuRebuildIndex);

                mnuRebuildIndex.addSelectionHandler(new SelectionHandler<Item>() {
                    @Override
                    public void onSelection(SelectionEvent<Item> event) {
                        rebuildHostIndex();
                    }
                });
            }

            if (mnuOfflineHost == null) {
                mnuOfflineHost = new MenuItem("Take host offline");
                contextMenu.add(mnuOfflineHost);

                mnuOfflineHost.addSelectionHandler(new SelectionHandler<Item>() {
                    @Override
                    public void onSelection(SelectionEvent<Item> event) {
                        offlineHost(true);
                    }
                });
            }

            if (mnuOnlineHost == null) {
                mnuOnlineHost = new MenuItem("Bring host online");
                contextMenu.add(mnuOnlineHost);

                mnuOnlineHost.addSelectionHandler(new SelectionHandler<Item>() {
                    @Override
                    public void onSelection(SelectionEvent<Item> event) {
                        offlineHost(false);
                    }
                });
            }
        }

    }


    private void rebuildHostIndex() {
        HostProxy host = selectionModel.getSelectedItem();
        if (host != null) {
            rf.hostService().rebuildIndex(host).fire();
            refresh();
        }
    }


    private void offlineHost(boolean offline) {
        HostProxy host = selectionModel.getSelectedItem();
        if (host != null) {
            HostServiceProxy req = rf.hostService();
            req.edit(host).setOffline(offline);
            req.fire();
        }
    }


    private void enableSelectionDependentControls(HostProxy hostInfo) {
        boolean enabled = hostInfo != null;
        boolean hostDisabled = hostInfo != null && !hostInfo.isEnabled();
        if (selectionDependentControlsEnabled != enabled) {
            btnRemoveHost.setEnabled(enabled && adminMode);
            btnEditHost.setEnabled(enabled && adminMode);
            btnListTraces.setEnabled(enabled);
            mnuRemoveHost.setEnabled(enabled && adminMode);
            mnuEditHost.setEnabled(enabled && adminMode);
            mnuListTraces.setEnabled(enabled);
            selectionDependentControlsEnabled = enabled;
        }

        btnDisableHost.setEnabled(enabled && !hostDisabled && adminMode);
        btnEnableHost.setEnabled(hostDisabled && adminMode);
        mnuDisableHost.setEnabled(enabled && !hostDisabled && adminMode);
        mnuEnableHost.setEnabled(hostDisabled && adminMode);

        btnAddHost.setEnabled(adminMode);
        mnuAddHost.setEnabled(adminMode);
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

        toolBar.add(new SeparatorToolItem());

        btnEditHost = new TextButton();
        btnEditHost.setIcon(Resources.INSTANCE.editIcon());
        btnEditHost.setToolTip("Edit host");
        btnEditHost.addSelectHandler(new SelectEvent.SelectHandler() {
            @Override
            public void onSelect(SelectEvent event) {
                editHost();
            }
        });
        toolBar.add(btnEditHost);

        toolBar.add(new SeparatorToolItem());

        btnDisableHost = new TextButton();
        btnDisableHost.setIcon(Resources.INSTANCE.disableIcon());
        btnDisableHost.setToolTip("Disable host");
        btnDisableHost.addSelectHandler(new SelectEvent.SelectHandler() {
            @Override
            public void onSelect(SelectEvent event) {
                toggleHost(false);
            }
        });
        toolBar.add(btnDisableHost);

        btnEnableHost = new TextButton();
        btnEnableHost.setIcon(Resources.INSTANCE.enableIcon());
        btnEnableHost.setToolTip("Enable host");
        btnEnableHost.addSelectHandler(new SelectEvent.SelectHandler() {
            @Override
            public void onSelect(SelectEvent event) {
                toggleHost(true);
            }
        });
        toolBar.add(btnEnableHost);

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

        ColumnConfig<HostProxy, HostProxy> nameCol = new ColumnConfig<HostProxy, HostProxy>(
                new IdentityValueProvider<HostProxy>(), 128, "Host Name");
        nameCol.setMenuDisabled(true);
        nameCol.setSortable(false);

        nameCol.setCell(new AbstractCell<HostProxy>() {
            @Override
            public void render(Context context, HostProxy host, SafeHtmlBuilder sb) {
                String color = (host.isEnabled() && !host.isOffline()) ? "black" : "gray";
                sb.appendHtmlConstant("<span style=\"color: " + color + ";\">");
                sb.append(SafeHtmlUtils.fromString(host.getName()));
                sb.appendHtmlConstant("</span>");
            }
        });

        ColumnConfig<HostProxy, HostProxy> addrCol = new ColumnConfig<HostProxy, HostProxy>(
                new IdentityValueProvider<HostProxy>(), 127, "IP Address");
        addrCol.setMenuDisabled(true);
        addrCol.setSortable(false);

        addrCol.setCell(new AbstractCell<HostProxy>() {
            @Override
            public void render(Context context, HostProxy host, SafeHtmlBuilder sb) {
                String color = (host.isEnabled() && !host.isOffline()) ? "black" : "gray";
                sb.appendHtmlConstant("<span style=\"color: " + color + ";\">");
                sb.append(SafeHtmlUtils.fromString(host.getAddr()));
                sb.appendHtmlConstant("</span>");
            }
        });

        ColumnModel<HostProxy> model = new ColumnModel<HostProxy>(Arrays.<ColumnConfig<HostProxy, ?>>asList(nameCol, addrCol));
        hostGridStore = new ListStore<HostProxy>(props.key());

        hostGrid = new Grid<HostProxy>(hostGridStore, model);
        selectionModel = hostGrid.getSelectionModel();
        selectionModel.setSelectionMode(Style.SelectionMode.SINGLE);

        hostGrid.getView().setAutoExpandColumn(addrCol);
        hostGrid.getView().setForceFit(true);

        // TODO host selection handler for keyboard: select item, press ENTER

        refresh();

        hostGrid.addCellDoubleClickHandler(new CellDoubleClickEvent.CellDoubleClickHandler() {
            @Override
            public void onCellClick(CellDoubleClickEvent event) {
                enableSelectionDependentControls(hostGrid.getSelectionModel().getSelectedItem());
                listTraces();
            }
        });

        hostGrid.addCellClickHandler(new CellClickEvent.CellClickHandler() {
            @Override
            public void onCellClick(CellClickEvent event) {
                enableSelectionDependentControls(hostGrid.getSelectionModel().getSelectedItem());
            }
        });

        add(hostGrid, new VerticalLayoutData(1, 1));
    }


    public void refresh() {
        hostGridStore.clear();
        rf.hostService().findAll().fire(new Receiver<List<HostProxy>>() {
            @Override
            public void onSuccess(List<HostProxy> response) {
                hostGridStore.addAll(response);
                enableSelectionDependentControls(hostGrid.getSelectionModel().getSelectedItem());
            }
        });
    }


    private void removeHost(final HostProxy hi) {
        if (hi != null) {
            ConfirmMessageBox cmb = new ConfirmMessageBox(
                    "Removing host", "Are you sure you want to remove host " + hi.getName() + "?");
            cmb.addHideHandler(new HideEvent.HideHandler() {
                @Override
                public void onHide(HideEvent event) {
                    Dialog d = (Dialog) event.getSource();
                    if ("Yes".equals(d.getHideButton().getText())) {
                        hostGridStore.remove(hi);
                        rf.hostService().remove(hi).fire();
                    }
                }
            });
            cmb.show();
        }
    }


    private void createContextMenu() {
        contextMenu = new Menu();

        mnuRefresh = new MenuItem("Refresh");
        mnuRefresh.setIcon(Resources.INSTANCE.refreshIcon());
        mnuRefresh.addSelectionHandler(new SelectionHandler<Item>() {
            @Override
            public void onSelection(SelectionEvent<Item> event) {
                refresh();
            }
        });
        contextMenu.add(mnuRefresh);

        contextMenu.add(new SeparatorMenuItem());

        mnuAddHost = new MenuItem("New host");
        mnuAddHost.setIcon(Resources.INSTANCE.addIcon());
        mnuAddHost.addSelectionHandler(new SelectionHandler<Item>() {
            @Override
            public void onSelection(SelectionEvent<Item> event) {
                addHost();
            }
        });
        contextMenu.add(mnuAddHost);

        mnuRemoveHost = new MenuItem("Remove host");
        mnuRemoveHost.setIcon(Resources.INSTANCE.removeIcon());
        mnuRemoveHost.addSelectionHandler(new SelectionHandler<Item>() {
            @Override
            public void onSelection(SelectionEvent<Item> event) {
                removeHost();
            }
        });
        contextMenu.add(mnuRemoveHost);

        mnuEditHost = new MenuItem("Edit host");
        mnuEditHost.setIcon(Resources.INSTANCE.editIcon());
        mnuEditHost.addSelectionHandler(new SelectionHandler<Item>() {
            @Override
            public void onSelection(SelectionEvent<Item> event) {
                editHost();
            }
        });
        contextMenu.add(mnuEditHost);

        contextMenu.add(new SeparatorMenuItem());

        mnuDisableHost = new MenuItem("Disable host");
        mnuDisableHost.setIcon(Resources.INSTANCE.disableIcon());
        mnuDisableHost.addSelectionHandler(new SelectionHandler<Item>() {
            @Override
            public void onSelection(SelectionEvent<Item> event) {
                toggleHost(false);
            }
        });
        contextMenu.add(mnuDisableHost);

        mnuEnableHost = new MenuItem("Enable host");
        mnuEnableHost.setIcon(Resources.INSTANCE.enableIcon());
        mnuEnableHost.addSelectionHandler(new SelectionHandler<Item>() {
            @Override
            public void onSelection(SelectionEvent<Item> event) {
                toggleHost(true);
            }
        });
        contextMenu.add(mnuEnableHost);

        contextMenu.add(new SeparatorMenuItem());

        mnuListTraces = new MenuItem("List traces");
        mnuListTraces.setIcon(Resources.INSTANCE.listColumnsIcon());
        mnuListTraces.addSelectionHandler(new SelectionHandler<Item>() {
            @Override
            public void onSelection(SelectionEvent<Item> event) {
                listTraces();
            }
        });
        contextMenu.add(mnuListTraces);

        hostGrid.setContextMenu(contextMenu);
    }


    private void addHost() {
        new HostPrefsDialog(rf, this, null, errorHandler).show();
    }


    private void removeHost() {
        removeHost(hostGrid.getSelectionModel().getSelectedItem());
    }


    private void editHost() {
        HostProxy hostInfo = hostGrid.getSelectionModel().getSelectedItem();
        if (hostInfo != null) {
            new HostPrefsDialog(rf, this, hostInfo, errorHandler).show();
        }
    }


    private void toggleHost(boolean enabled) {
        HostProxy info = selectionModel.getSelectedItem();
        if (info != null) {
            HostServiceProxy req = rf.hostService();
            HostProxy editedHost = req.edit(info);
            editedHost.setEnabled(enabled);
            req.fire(new Receiver<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    refresh();
                }
            });
        }
    }


    private void listTraces() {
        HostProxy hostInfo = selectionModel.getSelectedItem();
        GWT.log("Selected host: " + hostInfo);

        if (hostInfo != null) {
            shell.get().addView(panelFactory.traceSearchPanel(hostInfo), hostInfo.getName() + ": traces");
        }
    }
}
