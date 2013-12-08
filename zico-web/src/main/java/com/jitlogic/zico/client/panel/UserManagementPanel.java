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
import com.google.gwt.dom.client.Style;
import com.google.gwt.editor.client.Editor;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.web.bindery.requestfactory.shared.Receiver;
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
import com.sencha.gxt.widget.core.client.button.TextButton;
import com.sencha.gxt.widget.core.client.container.VerticalLayoutContainer;
import com.sencha.gxt.widget.core.client.event.*;
import com.sencha.gxt.widget.core.client.form.CheckBox;
import com.sencha.gxt.widget.core.client.form.TextField;
import com.sencha.gxt.widget.core.client.grid.CheckBoxSelectionModel;
import com.sencha.gxt.widget.core.client.grid.ColumnConfig;
import com.sencha.gxt.widget.core.client.grid.ColumnModel;
import com.sencha.gxt.widget.core.client.grid.Grid;
import com.sencha.gxt.widget.core.client.grid.editing.ClicksToEdit;
import com.sencha.gxt.widget.core.client.grid.editing.GridRowEditing;
import com.sencha.gxt.widget.core.client.toolbar.SeparatorToolItem;
import com.sencha.gxt.widget.core.client.toolbar.ToolBar;

import javax.inject.Inject;
import java.util.*;


public class UserManagementPanel extends VerticalLayoutContainer implements Editor<UserProxy> {

    private VerticalLayoutContainer pnlUserList;
    private GridRowEditing<UserProxy> userEditor;
    private TextField txtUserName;
    private CheckBox cbxUserAdmin;
    private TextField txtRealName;

    interface UserProperties extends PropertyAccess<UserProxy> {
        ModelKeyProvider<UserProxy> id();

        ValueProvider<UserProxy, String> userName();

        ValueProvider<UserProxy, String> realName();

        ValueProvider<UserProxy, Boolean> admin();
    }

    interface HostProperties extends PropertyAccess<HostProxy> {
        ModelKeyProvider<HostProxy> id();

        ValueProvider<HostProxy, String> name();
    }

    private static UserProperties userProperties = GWT.create(UserProperties.class);
    private static HostProperties hostProperties = GWT.create(HostProperties.class);

    private DockLayoutPanel rootPanel;

    private ZicoRequestFactory rf;
    private PanelFactory panelFactory;
    private UserServiceProxy newUserRequest;

    private ListStore<UserProxy> userStore;
    private Grid<UserProxy> userGrid;

    private UserProxy selectedUser;

    private List<HostProxy> hosts = new ArrayList<HostProxy>();
    private Set<Integer> allowedHosts = new HashSet<Integer>();

    private ListStore<HostProxy> hostStore;
    private CheckBoxSelectionModel<HostProxy> hostSelection;
    private Grid<HostProxy> hostGrid;

    @Inject
    public UserManagementPanel(ZicoRequestFactory requestFactory, PanelFactory panelFactory) {

        this.rf = requestFactory;
        this.panelFactory = panelFactory;

        loadHosts();
        createUi();
        refreshUsers();
    }


    private void createUi() {
        rootPanel = new DockLayoutPanel(Style.Unit.PCT);

        pnlUserList = new VerticalLayoutContainer();

        createToolBar();

        createUserGrid();

        createHostGrid();

        rootPanel.add(pnlUserList);
        add(rootPanel, new VerticalLayoutData(1, 1));
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


        pnlUserList.add(toolBar, new VerticalLayoutData(1, -1));
    }


    private void createUserGrid() {
        userStore = new ListStore<UserProxy>(userProperties.id());

        ColumnConfig<UserProxy, String> colUserName
            = new ColumnConfig<UserProxy, String>(userProperties.userName(), 100, "User");
        colUserName.setMenuDisabled(true);
        colUserName.setSortable(false);
        colUserName.setAlignment(HasHorizontalAlignment.ALIGN_CENTER);

        ColumnConfig<UserProxy, Boolean> colUserAdmin
            = new ColumnConfig<UserProxy, Boolean>(userProperties.admin(), 20, "Adm");
        colUserAdmin.setMenuDisabled(true);
        colUserAdmin.setSortable(false);
        colUserAdmin.setCell(new AbstractCell<Boolean>() {
            @Override
            public void render(Context context, Boolean val, SafeHtmlBuilder sb) {
                sb.appendHtmlConstant("<span>");
                sb.append(SafeHtmlUtils.fromString(val ? "yes" : "no"));
                sb.appendHtmlConstant("</span>");
            }
        });
        colUserAdmin.setAlignment(HasHorizontalAlignment.ALIGN_CENTER);

        ColumnConfig<UserProxy, String> colRealName
            = new ColumnConfig<UserProxy, String>(userProperties.realName(), 300, "Real Name");
        colRealName.setMenuDisabled(true);
        colRealName.setSortable(false);

        ColumnModel<UserProxy> model = new ColumnModel<UserProxy>(Arrays.<ColumnConfig<UserProxy,?>>asList(
                colUserName, colUserAdmin, colRealName));

        userGrid = new Grid<UserProxy>(userStore, model);
        userGrid.getView().setForceFit(true);
        userGrid.getView().setAutoFill(true);
        userGrid.getView().setAutoExpandColumn(colRealName);

        userGrid.addCellClickHandler(new CellClickEvent.CellClickHandler() {
            @Override
            public void onCellClick(CellClickEvent event) {
                UserProxy currentUser = userGrid.getSelectionModel().getSelectedItem();
                if (currentUser == selectedUser) { return; }
                selectedUser = currentUser;
                hostStore.clear();
                rf.userService().getAllowedHostIds(userGrid.getSelectionModel().getSelectedItem().getId()).fire(
                    new Receiver<List<Integer>>() {
                        @Override
                        public void onSuccess(List<Integer> hostIds) {
                            updateHosts(hostIds);
                        }
                    }
                );
            }
        });


        userEditor = new GridRowEditing<UserProxy>(userGrid);
        txtUserName = new TextField();
        userEditor.addEditor(colUserName, txtUserName);
        cbxUserAdmin = new CheckBox();
        userEditor.addEditor(colUserAdmin, cbxUserAdmin);
        txtRealName = new TextField();
        userEditor.addEditor(colRealName, txtRealName);
        userEditor.setClicksToEdit(ClicksToEdit.TWO);


//        userEditor.addStartEditHandler(new StartEditEvent.StartEditHandler<UserProxy>() {
//            @Override
//            public void onStartEdit(StartEditEvent<UserProxy> event) {
//
//            }
//        });

        userEditor.addCancelEditHandler(new CancelEditEvent.CancelEditHandler<UserProxy>() {
            @Override
            public void onCancelEdit(CancelEditEvent<UserProxy> event) {
                newUserRequest = null;
                refreshUsers();
            }
        });


        userEditor.addCompleteEditHandler(new CompleteEditEvent.CompleteEditHandler<UserProxy>() {
            @Override
            public void onCompleteEdit(CompleteEditEvent<UserProxy> event) {
                UserServiceProxy req = newUserRequest != null ? newUserRequest : rf.userService();
                UserProxy user = userStore.get(event.getEditCell().getRow());
                UserProxy editedUser = user.getId() != null ? req.edit(user) : user;
                editedUser.setUserName(txtUserName.getText());
                editedUser.setRealName(txtRealName.getText());
                editedUser.setAdmin(cbxUserAdmin.getValue());
                req.persist(editedUser).fire();
                refreshUsers();
            }
        });

        pnlUserList.add(userGrid, new VerticalLayoutData(1, 1));
    }


    private void addUser() {
        newUserRequest = rf.userService();
        userStore.add(0, newUserRequest.create(UserProxy.class));
        userEditor.startEditing(new Grid.GridCell(0, 1));
    }


    private void removeUser() {
        UserProxy user = userGrid.getSelectionModel().getSelectedItem();
        if (user != null) {
            userStore.remove(user);
            rf.userService().remove(user).fire();
            //refreshUsers();
        }
    }


    private void changePassword() {
        UserProxy user = userGrid.getSelectionModel().getSelectedItem();
        if (user != null) {
            PasswordChangeDialog dialog = panelFactory.passwordChangeDialog(user.getUserName());
            dialog.show();
        }
    }


    private void updateHosts(List<Integer> hostIds) {
        hostStore.clear();
        hostStore.addAll(hosts);
        allowedHosts.clear();
        allowedHosts.addAll(hostIds);
        Set<Integer> hostIdSet = new HashSet<Integer>();
        hostIdSet.addAll(hostIds);
        List<HostProxy> selectedHosts = new ArrayList<HostProxy>();
        for (HostProxy host : hosts) {
            if (hostIdSet.contains(host.getId())) {
                selectedHosts.add(host);
            }
        }
        hostSelection.setSelection(selectedHosts);
    }


    private void createHostGrid() {

        IdentityValueProvider<HostProxy> idp = new IdentityValueProvider<HostProxy>();

        hostSelection = new CheckBoxSelectionModel<HostProxy>(idp);
        hostSelection.setSelectionMode(com.sencha.gxt.core.client.Style.SelectionMode.SIMPLE);

        hostStore = new ListStore<HostProxy>(hostProperties.id());

        ColumnConfig<HostProxy, String> colHostName
            = new ColumnConfig<HostProxy, String>(hostProperties.name(), 100, "Host name");

        ColumnModel<HostProxy> hostModel
            = new ColumnModel<HostProxy>(Arrays.<ColumnConfig<HostProxy,?>>asList(hostSelection.getColumn(), colHostName));

        hostGrid = new Grid<HostProxy>(hostStore, hostModel);
        hostGrid.setSelectionModel(hostSelection);
        hostGrid.getView().setAutoExpandColumn(colHostName);
        hostGrid.getView().setForceFit(true);

        hostSelection.addSelectionHandler(new SelectionHandler<HostProxy>() {
            @Override
            public void onSelection(SelectionEvent<HostProxy> hostProxySelectionEvent) {
                saveAllowedHostList();
            }
        });

        ContentPanel cp = new ContentPanel();
        cp.setHeadingText("Allowed hosts");
        cp.add(hostGrid);

        rootPanel.addEast(cp, 25);
    }


    private void saveAllowedHostList() {
        UserProxy selectedUser = userGrid.getSelectionModel().getSelectedItem();

        if (selectedUser == null) {
            return;
        }

        List<Integer> ids = new ArrayList<Integer>();

        for (HostProxy host : hostSelection.getSelectedItems()) {
            ids.add(host.getId());
        }

        rf.userService().setAllowedHostIds(selectedUser.getId(), ids).fire();
    }


    private void refreshUsers() {
        hostStore.clear();
        userStore.clear();
        rf.userService().findAll().fire(new Receiver<List<UserProxy>>() {
            @Override
            public void onSuccess(List<UserProxy> users) {
                userStore.addAll(users);
            }
        });
    }


    private void loadHosts() {
        rf.hostService().findAll().fire(new Receiver<List<HostProxy>>() {
            @Override
            public void onSuccess(List<HostProxy> hosts) {
                UserManagementPanel.this.hosts = hosts;
            }
        });
    }
}
