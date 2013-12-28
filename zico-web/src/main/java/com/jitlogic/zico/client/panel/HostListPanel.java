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
import com.google.gwt.cell.client.Cell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ContextMenuEvent;
import com.google.gwt.event.dom.client.ContextMenuHandler;
import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.event.dom.client.DoubleClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.user.cellview.client.HasKeyboardSelectionPolicy;
import com.google.gwt.user.cellview.client.IdentityColumn;
import com.google.gwt.view.client.CellPreviewEvent;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SingleSelectionModel;
import com.google.inject.Inject;
import com.google.web.bindery.requestfactory.shared.Receiver;
import com.jitlogic.zico.client.ErrorHandler;
import com.jitlogic.zico.client.Resources;
import com.jitlogic.zico.client.ZicoShell;
import com.jitlogic.zico.client.inject.PanelFactory;
import com.jitlogic.zico.client.inject.ZicoRequestFactory;
import com.jitlogic.zico.shared.data.HostProxy;
import com.jitlogic.zico.shared.services.HostServiceProxy;
import com.sencha.gxt.widget.core.client.Dialog;
import com.sencha.gxt.widget.core.client.box.ConfirmMessageBox;
import com.sencha.gxt.widget.core.client.button.TextButton;
import com.sencha.gxt.widget.core.client.container.VerticalLayoutContainer;
import com.sencha.gxt.widget.core.client.event.HideEvent;
import com.sencha.gxt.widget.core.client.event.SelectEvent;
import com.sencha.gxt.widget.core.client.menu.Item;
import com.sencha.gxt.widget.core.client.menu.Menu;
import com.sencha.gxt.widget.core.client.menu.MenuItem;
import com.sencha.gxt.widget.core.client.menu.SeparatorMenuItem;
import com.sencha.gxt.widget.core.client.toolbar.SeparatorToolItem;
import com.sencha.gxt.widget.core.client.toolbar.ToolBar;

import javax.inject.Provider;
import java.util.List;


public class HostListPanel extends VerticalLayoutContainer {

    private Provider<ZicoShell> shell;
    private PanelFactory panelFactory;
    private ZicoRequestFactory rf;

    private DataGrid<HostProxy> hostGrid;
    private ListDataProvider<HostProxy> hostGridStore;
    private SingleSelectionModel<HostProxy> selectionModel;

    private ErrorHandler errorHandler;

    private TextButton btnRefresh, btnAddHost, btnRemoveHost, btnEditHost, btnListTraces, btnDisableHost, btnEnableHost;
    private MenuItem mnuRefresh, mnuAddHost, mnuRemoveHost, mnuEditHost, mnuListTraces, mnuDisableHost, mnuEnableHost;

    private boolean selectionDependentControlsEnabled = true;

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

    private static final ProvidesKey<HostProxy> KEY_PROVIDER = new ProvidesKey<HostProxy>() {
        @Override
        public Object getKey(HostProxy item) {
            return item.getName();
        }
    };

    private static final Cell<HostProxy> NAME_CELL = new AbstractCell<HostProxy>() {
        @Override
        public void render(Context context, HostProxy host, SafeHtmlBuilder sb) {
            String color = (host.isEnabled()) ? "black" : "gray";
            sb.appendHtmlConstant("<span style=\"color: " + color + ";\">");
            sb.append(SafeHtmlUtils.fromString(host.getName()));
            sb.appendHtmlConstant("</span>");
        }
    };

    private static final Cell<HostProxy> ADDRESS_CELL = new AbstractCell<HostProxy>() {
        @Override
        public void render(Context context, HostProxy host, SafeHtmlBuilder sb) {
            String color = (host.isEnabled()) ? "black" : "gray";
            sb.appendHtmlConstant("<span style=\"color: " + color + ";\">");
            sb.append(SafeHtmlUtils.fromString(host.getAddr()));
            sb.appendHtmlConstant("</span>");
        }
    };

    private void createHostListPanel() {

        hostGrid = new DataGrid<HostProxy>(1024*1024, KEY_PROVIDER);
        selectionModel = new SingleSelectionModel<HostProxy>(KEY_PROVIDER);
        hostGrid.setSelectionModel(selectionModel);

        Column<HostProxy,HostProxy> colName = new IdentityColumn<HostProxy>(NAME_CELL);
        hostGrid.addColumn(colName, new ResizableHeader<HostProxy>("Name", hostGrid, colName));
        hostGrid.setColumnWidth(colName, 128, Style.Unit.PX);

        Column<HostProxy,HostProxy> colAddr = new IdentityColumn<HostProxy>(ADDRESS_CELL);
        hostGrid.addColumn(colAddr, "Address");
        hostGrid.setColumnWidth(colAddr, 100, Style.Unit.PCT);

        hostGrid.setSkipRowHoverStyleUpdate(true);
        hostGrid.setSkipRowHoverFloatElementCheck(true);
        hostGrid.setSkipRowHoverCheck(true);
        hostGrid.setKeyboardSelectionPolicy(HasKeyboardSelectionPolicy.KeyboardSelectionPolicy.DISABLED);

        hostGridStore = new ListDataProvider<HostProxy>();
        hostGridStore.addDataDisplay(hostGrid);

        hostGrid.addCellPreviewHandler(new CellPreviewEvent.Handler<HostProxy>() {
            @Override
            public void onCellPreview(CellPreviewEvent<HostProxy> event) {
                NativeEvent nev = event.getNativeEvent();
                String eventType = nev.getType();
                if ((BrowserEvents.KEYDOWN.equals(eventType) && nev.getKeyCode() == KeyCodes.KEY_ENTER)
                        || BrowserEvents.DBLCLICK.equals(nev.getType())) {
                    selectionModel.setSelected(event.getValue(), true);
                    enableSelectionDependentControls(event.getValue());
                    listTraces();
                }
                if (BrowserEvents.CONTEXTMENU.equals(eventType)) {
                    selectionModel.setSelected(event.getValue(), true);
                    if (event.getValue() != null) {
                        contextMenu.showAt(event.getNativeEvent().getClientX(), event.getNativeEvent().getClientY());
                    }
                }

                // TODO update toolbar icons and context menu on element selection
            }
        });

        hostGrid.addDomHandler(new DoubleClickHandler() {
            @Override
            public void onDoubleClick(DoubleClickEvent event) {
                event.preventDefault();
            }
        }, DoubleClickEvent.getType());

        hostGrid.addDomHandler(new ContextMenuHandler() {
            @Override
            public void onContextMenu(ContextMenuEvent event) {
                event.preventDefault();
            }
        }, ContextMenuEvent.getType());

        refresh();

        add(hostGrid, new VerticalLayoutData(1, 1));
    }


    public void refresh() {
        hostGridStore.getList().clear();
        rf.hostService().findAll().fire(new Receiver<List<HostProxy>>() {
            @Override
            public void onSuccess(List<HostProxy> response) {
                hostGridStore.getList().addAll(response);
                enableSelectionDependentControls(selectionModel.getSelectedObject());
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
                        hostGridStore.getList().remove(hi);
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

        //hostGrid.setContextMenu(contextMenu);
    }


    private void addHost() {
        new HostPrefsDialog(rf, this, null, errorHandler).show();
    }


    private void removeHost() {
        removeHost(selectionModel.getSelectedObject());
    }


    private void editHost() {
        HostProxy hostInfo = selectionModel.getSelectedObject();
        if (hostInfo != null) {
            new HostPrefsDialog(rf, this, hostInfo, errorHandler).show();
        }
    }


    private void toggleHost(boolean enabled) {
        HostProxy info = selectionModel.getSelectedObject();
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
        HostProxy hostInfo = selectionModel.getSelectedObject();
        GWT.log("Selected host: " + hostInfo);

        if (hostInfo != null) {
            shell.get().addView(panelFactory.traceSearchPanel(hostInfo), hostInfo.getName() + ": traces");
        }
    }
}
