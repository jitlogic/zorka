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
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.web.bindery.requestfactory.shared.Receiver;
import com.jitlogic.zico.client.Resources;
import com.jitlogic.zico.client.inject.ZicoRequestFactory;
import com.jitlogic.zico.data.*;
import com.sencha.gxt.core.client.ValueProvider;
import com.sencha.gxt.data.shared.ListStore;
import com.sencha.gxt.data.shared.ModelKeyProvider;
import com.sencha.gxt.data.shared.PropertyAccess;
import com.sencha.gxt.widget.core.client.ContentPanel;
import com.sencha.gxt.widget.core.client.button.TextButton;
import com.sencha.gxt.widget.core.client.container.VerticalLayoutContainer;
import com.sencha.gxt.widget.core.client.event.CellClickEvent;
import com.sencha.gxt.widget.core.client.grid.ColumnConfig;
import com.sencha.gxt.widget.core.client.grid.ColumnModel;
import com.sencha.gxt.widget.core.client.grid.Grid;
import com.sencha.gxt.widget.core.client.toolbar.ToolBar;

import javax.inject.Inject;
import java.util.*;


public class UserManagementPanel extends VerticalLayoutContainer {

    interface UserProperties extends PropertyAccess<UserProxy> {
        ModelKeyProvider<UserProxy> id();

        ValueProvider<UserProxy, String> userName();

        ValueProvider<UserProxy, String> realName();
    }

    interface HostProperties extends PropertyAccess<HostProxy> {
        ModelKeyProvider<HostProxy> id();

        ValueProvider<HostProxy, String> name();
    }

    private static UserProperties userProperties = GWT.create(UserProperties.class);
    private static HostProperties hostProperties = GWT.create(HostProperties.class);

    private DockLayoutPanel rootPanel;

    private UserServiceProxy userService;
    private HostServiceProxy hostService;

    private ListStore<UserProxy> userStore;
    private Grid<UserProxy> userGrid;

    private List<HostProxy> hosts = new ArrayList<HostProxy>();
    private Set<Integer> allowedHosts = new HashSet<Integer>();

    private ListStore<HostProxy> hostStore;
    private Grid<HostProxy> hostGrid;


    @Inject
    public UserManagementPanel(ZicoRequestFactory requestFactory) {

        this.userService = requestFactory.userService();
        this.hostService = requestFactory.hostService();

        loadHosts();
        createUi();
        refreshUsers();
    }


    private void createUi() {
        rootPanel = new DockLayoutPanel(Style.Unit.PCT);

        VerticalLayoutContainer vp = new VerticalLayoutContainer();

        ToolBar toolBar = new ToolBar();

        TextButton btnRefresh = new TextButton();
        btnRefresh.setIcon(Resources.INSTANCE.refreshIcon());
        btnRefresh.setToolTip("Refresh user list");
        toolBar.add(btnRefresh);

        vp.add(toolBar, new VerticalLayoutData(1, -1));

        createUserGrid(vp);

        createHostGrid();

        rootPanel.add(vp);
        add(rootPanel, new VerticalLayoutData(1, 1));
    }

    private void createUserGrid(VerticalLayoutContainer vp) {
        userStore = new ListStore<UserProxy>(userProperties.id());

        ColumnConfig<UserProxy, String> colUserName
            = new ColumnConfig<UserProxy, String>(userProperties.userName(), 100, "Username");
        colUserName.setMenuDisabled(true);
        colUserName.setSortable(false);

        ColumnConfig<UserProxy, String> colRealName
            = new ColumnConfig<UserProxy, String>(userProperties.realName(), 100, "Real Name");
        colRealName.setMenuDisabled(true);
        colRealName.setSortable(false);

        ColumnModel<UserProxy> model = new ColumnModel<UserProxy>(Arrays.<ColumnConfig<UserProxy,?>>asList(
                colUserName, colRealName));

        userGrid = new Grid<UserProxy>(userStore, model);

        userGrid.addCellClickHandler(new CellClickEvent.CellClickHandler() {
            @Override
            public void onCellClick(CellClickEvent event) {
                updateHostList();
            }
        });

        vp.add(userGrid);
    }


    private void updateHostList() {
        hostStore.clear();
        userService.findAllowedHostIds(userGrid.getSelectionModel().getSelectedItem().getId()).fire(
                new Receiver<List<Integer>>() {
                    @Override
                    public void onSuccess(List<Integer> hostIds) {
                        hostStore.addAll(hosts);
                        allowedHosts.clear();
                        allowedHosts.addAll(hostIds);
                    }
                }
        );
    }


    private void createHostGrid() {
        hostStore = new ListStore<HostProxy>(hostProperties.id());

        ColumnConfig<HostProxy, String> colHostName
            = new ColumnConfig<HostProxy, String>(hostProperties.name(), 100, "Host name");

        ColumnModel<HostProxy> hostModel
            = new ColumnModel<HostProxy>(Arrays.<ColumnConfig<HostProxy,?>>asList(colHostName));

        hostGrid = new Grid<HostProxy>(hostStore, hostModel);

        ContentPanel cp = new ContentPanel();
        cp.setHeadingText("Allowed hosts");
        cp.add(hostGrid);

        rootPanel.addEast(cp, 25);
    }


    private void refreshUsers() {
        userService.findAll().fire(new Receiver<List<UserProxy>>() {
            @Override
            public void onSuccess(List<UserProxy> users) {
                userStore.addAll(users);
            }
        });
    }


    private void loadHosts() {
        hostService.findAll().fire(new Receiver<List<HostProxy>>() {
            @Override
            public void onSuccess(List<HostProxy> hosts) {
                UserManagementPanel.this.hosts = hosts;
            }
        });
    }
}
