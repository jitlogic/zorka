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


import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.web.bindery.requestfactory.shared.Receiver;
import com.google.web.bindery.requestfactory.shared.ServerFailure;
import com.jitlogic.zico.client.ErrorHandler;
import com.jitlogic.zico.client.inject.ZicoRequestFactory;
import com.jitlogic.zico.shared.data.UserProxy;
import com.jitlogic.zico.shared.services.UserServiceProxy;
import com.sencha.gxt.widget.core.client.Dialog;
import com.sencha.gxt.widget.core.client.button.TextButton;
import com.sencha.gxt.widget.core.client.container.VerticalLayoutContainer;
import com.sencha.gxt.widget.core.client.event.SelectEvent;
import com.sencha.gxt.widget.core.client.form.CheckBox;
import com.sencha.gxt.widget.core.client.form.FieldLabel;
import com.sencha.gxt.widget.core.client.form.TextField;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class UserPrefsDialog extends Dialog {

    private UserServiceProxy editUserRequest;
    private UserProxy editedUser;

    private ErrorHandler errorHandler;

    public UserManagementPanel panel;

    private TextField txtUsername;
    private TextField txtRealName;
    private CheckBox chkIsAdmin;

    private List<String> availableHosts;

    private Map<String,CheckBox> selectedHosts = new HashMap<String, CheckBox>();


    public UserPrefsDialog(ZicoRequestFactory rf, UserProxy user, UserManagementPanel panel,
                           List<String> availableHosts, ErrorHandler errorHandler) {
        editUserRequest = rf.userService();
        this.editedUser = user != null ? editUserRequest.edit(user) : editUserRequest.create(UserProxy.class);
        this.panel = panel;
        this.errorHandler = errorHandler;

        this.availableHosts = availableHosts;

        setHeadingText(user != null ? "Edit user: " + user.getUserName() : "New user");

        createUi(user);
    }


    private void createUi(UserProxy user) {

        setPredefinedButtons();

        VerticalLayoutContainer vlc = new VerticalLayoutContainer();

        txtUsername = new TextField();
        txtUsername.setAllowBlank(false);
        vlc.add(txtUsername);
        vlc.add(new FieldLabel(txtUsername, "Username"),
                new VerticalLayoutContainer.VerticalLayoutData(1, -1));

        if (user != null) {
            txtUsername.setText(user.getUserName());
            txtUsername.setEnabled(false);
        }

        txtRealName = new TextField();
        txtRealName.setAllowBlank(false);
        vlc.add(txtRealName);
        vlc.add(new FieldLabel(txtRealName, "Real Name"),
                new VerticalLayoutContainer.VerticalLayoutData(1, -1));

        if (user != null) {
            txtRealName.setText(user.getRealName());
        }

        chkIsAdmin = new CheckBox();
        chkIsAdmin.setBoxLabel("Administrator role");
        vlc.add(chkIsAdmin);
        vlc.add(new FieldLabel(chkIsAdmin, "Roles"),
                new VerticalLayoutContainer.VerticalLayoutData(1, -1));

        if (user != null) {
            chkIsAdmin.setValue(user.isAdmin());
        }

        VerticalLayoutContainer vlh = new VerticalLayoutContainer();

        Set<String> hosts = new HashSet<String>();

        if (user != null && user.getAllowedHosts() != null) {
            hosts.addAll(user.getAllowedHosts());
        }

        for (String host : availableHosts) {
            CheckBox chkHost = new CheckBox();
            chkHost.setValue(hosts.contains(host));
            chkHost.setBoxLabel(host);
            selectedHosts.put(host, chkHost);
            vlh.add(chkHost);
        }

        ScrollPanel scrHosts = new ScrollPanel(vlh);

        vlc.add(scrHosts);

        scrHosts.setWidth("100%"); scrHosts.setHeight("100%");

        setWidth(400); setHeight(500);
        setHideOnButtonClick(false);
        add(vlc);

        TextButton btnOk = new TextButton("OK");
        addButton(btnOk);

        btnOk.addSelectHandler(new SelectEvent.SelectHandler() {
            @Override
            public void onSelect(SelectEvent event) {
                save();
            }
        });

        TextButton btnCancel = new TextButton("Cancel");
        addButton(btnCancel);

        btnCancel.addSelectHandler(new SelectEvent.SelectHandler() {
            @Override
            public void onSelect(SelectEvent event) {
                hide();
            }
        });

    }


    private void save() {
        if (editedUser.getUserName() == null) {
            editedUser.setUserName(txtUsername.getText());
        }
        editedUser.setRealName(txtRealName.getText());
        editedUser.setAdmin(chkIsAdmin.getValue());

        List<String> hosts = new ArrayList<String>(selectedHosts.size());

        for (Map.Entry<String,CheckBox> e : selectedHosts.entrySet()) {
            if (e.getValue().getValue()) {
                hosts.add(e.getKey());
            }
        }

        Collections.sort(hosts);
        editedUser.setAllowedHosts(hosts);

        editUserRequest.persist(editedUser).fire(new Receiver<Void>() {
            @Override
            public void onSuccess(Void response) {
                hide();
                panel.refreshUsers();
            }
            @Override
            public void onFailure(ServerFailure failure) {
                errorHandler.error("Error saving user data", failure);
            }
        });
    }
}
