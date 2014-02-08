/**
 * Copyright 2012-2014 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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
import com.google.gwt.editor.client.Editor;
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
import com.google.gwt.user.cellview.client.IdentityColumn;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.view.client.CellPreviewEvent;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SingleSelectionModel;
import com.google.web.bindery.requestfactory.shared.Receiver;
import com.google.web.bindery.requestfactory.shared.ServerFailure;
import com.jitlogic.zico.client.ErrorHandler;
import com.jitlogic.zico.client.Resources;
import com.jitlogic.zico.client.inject.PanelFactory;
import com.jitlogic.zico.client.inject.ZicoRequestFactory;
import com.jitlogic.zico.shared.data.HostProxy;
import com.jitlogic.zico.shared.data.UserProxy;
import com.jitlogic.zico.shared.services.UserServiceProxy;
import com.sencha.gxt.core.client.IdentityValueProvider;
import com.sencha.gxt.core.client.ValueProvider;
import com.sencha.gxt.data.shared.ListStore;
import com.sencha.gxt.data.shared.ModelKeyProvider;
import com.sencha.gxt.data.shared.PropertyAccess;
import com.sencha.gxt.widget.core.client.ContentPanel;
import com.sencha.gxt.widget.core.client.Dialog;
import com.sencha.gxt.widget.core.client.box.ConfirmMessageBox;
import com.sencha.gxt.widget.core.client.button.TextButton;
import com.sencha.gxt.widget.core.client.container.VerticalLayoutContainer;
import com.sencha.gxt.widget.core.client.event.CancelEditEvent;
import com.sencha.gxt.widget.core.client.event.CellClickEvent;
import com.sencha.gxt.widget.core.client.event.CompleteEditEvent;
import com.sencha.gxt.widget.core.client.event.HideEvent;
import com.sencha.gxt.widget.core.client.event.SelectEvent;
import com.sencha.gxt.widget.core.client.form.CheckBox;
import com.sencha.gxt.widget.core.client.form.TextField;
import com.sencha.gxt.widget.core.client.grid.CheckBoxSelectionModel;
import com.sencha.gxt.widget.core.client.grid.ColumnConfig;
import com.sencha.gxt.widget.core.client.grid.ColumnModel;
import com.sencha.gxt.widget.core.client.grid.Grid;
import com.sencha.gxt.widget.core.client.grid.editing.ClicksToEdit;
import com.sencha.gxt.widget.core.client.grid.editing.GridRowEditing;
import com.sencha.gxt.widget.core.client.menu.Item;
import com.sencha.gxt.widget.core.client.menu.Menu;
import com.sencha.gxt.widget.core.client.menu.MenuItem;
import com.sencha.gxt.widget.core.client.menu.SeparatorMenuItem;
import com.sencha.gxt.widget.core.client.toolbar.SeparatorToolItem;
import com.sencha.gxt.widget.core.client.toolbar.ToolBar;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class UserManagementPanel extends VerticalLayoutContainer {

    private ZicoRequestFactory rf;
    private PanelFactory panelFactory;
    private ErrorHandler errorHandler;

    private ListDataProvider<UserProxy> userStore;
    private DataGrid<UserProxy> userGrid;
    private SingleSelectionModel<UserProxy> selectionModel;

    private Menu contextMenu;

    private List<String> hostNames = new ArrayList<String>();

    @Inject
    public UserManagementPanel(ZicoRequestFactory requestFactory, PanelFactory panelFactory, ErrorHandler errorHandler) {

        this.rf = requestFactory;
        this.panelFactory = panelFactory;
        this.errorHandler = errorHandler;

        loadHosts();
        createUi();
        refreshUsers();
    }


    private void createUi() {
        createToolBar();
        createUserGrid();
        createContextMenu();
    }


    private void createToolBar() {
        ToolBar toolBar = new ToolBar();

        TextButton btnRefresh = new TextButton();
        btnRefresh.setIcon(Resources.INSTANCE.refreshIcon());
        btnRefresh.setToolTip("Refresh user list");
        toolBar.add(btnRefresh);

        btnRefresh.addSelectHandler(new SelectEvent.SelectHandler() {
            @Override
            public void onSelect(SelectEvent event) {
                refreshUsers();
            }
        });

        toolBar.add(new SeparatorToolItem());

        TextButton btnAdd = new TextButton();
        btnAdd.setIcon(Resources.INSTANCE.addIcon());
        btnAdd.setToolTip("Add user");
        toolBar.add(btnAdd);

        btnAdd.addSelectHandler(new SelectEvent.SelectHandler() {
            @Override
            public void onSelect(SelectEvent event) {
                addUser();
            }
        });

        TextButton btnRemove = new TextButton();
        btnRemove.setIcon(Resources.INSTANCE.removeIcon());
        btnRemove.setToolTip("Remove user");
        toolBar.add(btnRemove);

        btnRemove.addSelectHandler(new SelectEvent.SelectHandler() {
            @Override
            public void onSelect(SelectEvent event) {
                removeUser();
            }
        });

        toolBar.add(new SeparatorToolItem());

        TextButton btnPassword = new TextButton();
        btnPassword.setIcon(Resources.INSTANCE.keyIcon());
        btnPassword.setToolTip("Change user password");
        toolBar.add(btnPassword);

        btnPassword.addSelectHandler(new SelectEvent.SelectHandler() {
            @Override
            public void onSelect(SelectEvent event) {
                changePassword();
            }
        });


        add(toolBar, new VerticalLayoutData(1, -1));
    }


    private final static ProvidesKey<UserProxy> KEY_PROVIDER = new ProvidesKey<UserProxy>() {
        @Override
        public Object getKey(UserProxy item) {
            return item.getUserName();
        }
    };

    private static final Cell<UserProxy> USERNAME_CELL = new AbstractCell<UserProxy>() {
        @Override
        public void render(Context context, UserProxy value, SafeHtmlBuilder sb) {
            sb.append(SafeHtmlUtils.fromString(value.getUserName()));
        }
    };

    private static final Cell<UserProxy> REALNAME_CELL = new AbstractCell<UserProxy>() {
        @Override
        public void render(Context context, UserProxy value, SafeHtmlBuilder sb) {
            sb.append(SafeHtmlUtils.fromString(value.getRealName()));
        }
    };

    private static final Cell<UserProxy> USERROLE_CELL = new AbstractCell<UserProxy>() {
        @Override
        public void render(Context context, UserProxy value, SafeHtmlBuilder sb) {
            sb.append(SafeHtmlUtils.fromString(value.isAdmin() ? "ADMIN" : "VIEWER"));
        }
    };

    private static final Cell<UserProxy> USERHOSTS_CELL = new AbstractCell<UserProxy>() {
        @Override
        public void render(Context context, UserProxy value, SafeHtmlBuilder sb) {
            if (value.isAdmin()) {
                sb.appendHtmlConstant(
                    "<span style=\"color: gray;\"> ** all hosts visible due to administrator privileges ** </span>");
            } else {
                List<String> hosts = value.getAllowedHosts();
                if (hosts != null) {
                    for (int i = 0; i < hosts.size(); i++) {
                        if (i > 0) {
                            sb.appendHtmlConstant(",");
                        }
                        sb.append(SafeHtmlUtils.fromString(hosts.get(i)));
                    }
                }
            }
        }
    };


    private void createUserGrid() {
        userGrid = new DataGrid<UserProxy>(1024 * 1024, KEY_PROVIDER);
        selectionModel = new SingleSelectionModel<UserProxy>(KEY_PROVIDER);
        userGrid.setSelectionModel(selectionModel);

        Column<UserProxy,UserProxy> colUsername = new IdentityColumn<UserProxy>(USERNAME_CELL);
        userGrid.addColumn(colUsername, new ResizableHeader<UserProxy>("Username", userGrid, colUsername));
        userGrid.setColumnWidth(colUsername, 128, Style.Unit.PX);

        Column<UserProxy,UserProxy> colUserRole = new IdentityColumn<UserProxy>(USERROLE_CELL);
        userGrid.addColumn(colUserRole, new ResizableHeader<UserProxy>("Role", userGrid, colUserRole));
        userGrid.setColumnWidth(colUserRole, 64, Style.Unit.PX);

        Column<UserProxy,UserProxy> colRealName = new IdentityColumn<UserProxy>(REALNAME_CELL);
        userGrid.addColumn(colRealName, new ResizableHeader<UserProxy>("Real Name", userGrid, colRealName));
        userGrid.setColumnWidth(colRealName, 256, Style.Unit.PX);

        Column<UserProxy,UserProxy> colUserHosts = new IdentityColumn<UserProxy>(USERHOSTS_CELL);
        userGrid.addColumn(colUserHosts, "Allowed hosts");
        userGrid.setColumnWidth(colUserHosts, 100, Style.Unit.PCT);

        userStore = new ListDataProvider<UserProxy>(KEY_PROVIDER);
        userStore.addDataDisplay(userGrid);

        userGrid.addCellPreviewHandler(new CellPreviewEvent.Handler<UserProxy>() {
            @Override
            public void onCellPreview(CellPreviewEvent<UserProxy> event) {
                NativeEvent nev = event.getNativeEvent();
                String eventType = nev.getType();
                if ((BrowserEvents.KEYDOWN.equals(eventType) && nev.getKeyCode() == KeyCodes.KEY_ENTER)
                        || BrowserEvents.DBLCLICK.equals(nev.getType())) {
                    selectionModel.setSelected(event.getValue(), true);
                    editUser();
                }
                if (BrowserEvents.CONTEXTMENU.equals(eventType)) {
                    selectionModel.setSelected(event.getValue(), true);
                    if (event.getValue() != null) {
                        contextMenu.showAt(event.getNativeEvent().getClientX(), event.getNativeEvent().getClientY());
                    }
                }

            }
        });

        userGrid.addDomHandler(new DoubleClickHandler() {
            @Override
            public void onDoubleClick(DoubleClickEvent event) {
                event.preventDefault();
            }
        }, DoubleClickEvent.getType());

        userGrid.addDomHandler(new ContextMenuHandler() {
            @Override
            public void onContextMenu(ContextMenuEvent event) {
                event.preventDefault();
            }
        }, ContextMenuEvent.getType());

        add(userGrid, new VerticalLayoutData(1, 1));
    }


    private void createContextMenu() {
        contextMenu = new Menu();

        MenuItem mnuRefresh = new MenuItem("Refresh");
        mnuRefresh.setIcon(Resources.INSTANCE.refreshIcon());
        mnuRefresh.addSelectionHandler(new SelectionHandler<Item>() {
            @Override
            public void onSelection(SelectionEvent<Item> event) {
                refreshUsers();
            }
        });

        contextMenu.add(new SeparatorMenuItem());

        MenuItem mnuAddUser = new MenuItem("Add user");
        mnuAddUser.setIcon(Resources.INSTANCE.addIcon());
        mnuAddUser.addSelectionHandler(new SelectionHandler<Item>() {
            @Override
            public void onSelection(SelectionEvent<Item> event) {
                addUser();
            }
        });
        contextMenu.add(mnuAddUser);

        MenuItem mnuRemoveUser = new MenuItem("Remove user");
        mnuRemoveUser.setIcon(Resources.INSTANCE.removeIcon());
        mnuRemoveUser.addSelectionHandler(new SelectionHandler<Item>() {
            @Override
            public void onSelection(SelectionEvent<Item> event) {
                removeUser();
            }
        });
        contextMenu.add(mnuRemoveUser);

        MenuItem mnuEditUser = new MenuItem("Edit user");
        mnuEditUser.setIcon(Resources.INSTANCE.editIcon());
        mnuEditUser.addSelectionHandler(new SelectionHandler<Item>() {
            @Override
            public void onSelection(SelectionEvent<Item> event) {
                editUser();
            }
        });

        contextMenu.add(new SeparatorMenuItem());

        MenuItem mnuChangePassword = new MenuItem("Change password");
        mnuChangePassword.setIcon(Resources.INSTANCE.keyIcon());
        mnuChangePassword.addSelectionHandler(new SelectionHandler<Item>() {
            @Override
            public void onSelection(SelectionEvent<Item> event) {
                changePassword();
            }
        });
    }


    private void editUser() {
        UserProxy user = selectionModel.getSelectedObject();
        if (user != null) {
            new UserPrefsDialog(rf, user, this, hostNames, errorHandler).show();
        }
    }


    private void addUser() {
        new UserPrefsDialog(rf, null, this, hostNames, errorHandler).show();
    }


    private void removeUser() {
        final UserProxy user = selectionModel.getSelectedObject();
        if (user != null) {
            ConfirmMessageBox cmb = new ConfirmMessageBox(
                    "Removing host", "Are you sure you want to remove " + user.getUserName() + " ?");
            cmb.addHideHandler(new HideEvent.HideHandler() {
                @Override
                public void onHide(HideEvent event) {
                    Dialog d = (Dialog) event.getSource();
                    if ("Yes".equals(d.getHideButton().getText())) {
                        userStore.getList().remove(user);
                        rf.userService().remove(user).fire(
                                new Receiver<Void>() {
                                    @Override
                                    public void onSuccess(Void response) {
                                        refreshUsers();
                                    }
                                    public void onFailure(ServerFailure failure) {
                                        errorHandler.error("Error removing user " + user.getUserName(), failure);
                                    }
                                }
                        );
                    }
                }
            });
            cmb.show();
        }
    }


    private void changePassword() {
        UserProxy user = selectionModel.getSelectedObject();
        if (user != null) {
            PasswordChangeDialog dialog = panelFactory.passwordChangeDialog(user.getUserName());
            dialog.show();
        }
    }


    public void refreshUsers() {
        userStore.getList().clear();
        rf.userService().findAll().fire(new Receiver<List<UserProxy>>() {
            @Override
            public void onSuccess(List<UserProxy> users) {
                userStore.getList().addAll(users);
            }
            @Override
            public void onFailure(ServerFailure failure) {
                errorHandler.error("Error loading user data", failure);
            }
        });
    }


    private void loadHosts() {
        rf.hostService().findAll().fire(new Receiver<List<HostProxy>>() {
            @Override
            public void onSuccess(List<HostProxy> hosts) {
                hostNames.clear();
                for (HostProxy h : hosts) {
                    hostNames.add(h.getName());
                }
                Collections.sort(hostNames);
            }
            @Override
            public void onFailure(ServerFailure failure) {
                errorHandler.error("Error loading user data", failure);
            }
        });
    }
}
