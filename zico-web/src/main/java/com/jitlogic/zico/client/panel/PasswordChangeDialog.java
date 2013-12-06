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


import com.jitlogic.zico.client.Resources;
import com.jitlogic.zico.client.api.UserApi;
import com.sencha.gxt.widget.core.client.Dialog;
import com.sencha.gxt.widget.core.client.box.AlertMessageBox;
import com.sencha.gxt.widget.core.client.button.TextButton;
import com.sencha.gxt.widget.core.client.container.VerticalLayoutContainer;
import com.sencha.gxt.widget.core.client.event.SelectEvent;
import com.sencha.gxt.widget.core.client.form.FieldLabel;
import com.sencha.gxt.widget.core.client.form.PasswordField;
import org.fusesource.restygwt.client.Method;
import org.fusesource.restygwt.client.MethodCallback;

import javax.inject.Inject;


public class PasswordChangeDialog extends Dialog {

    private UserApi userApi;

    private PasswordField txtOldPassword, txtNewPassword, txtRepPassword;

    @Inject
    public PasswordChangeDialog(UserApi userApi) {
        this.userApi = userApi;
        createUi();
    }


    private void createUi() {

        setPredefinedButtons();

        VerticalLayoutContainer vlc = new VerticalLayoutContainer();

        txtOldPassword =  new PasswordField();
        vlc.add(txtOldPassword);
        vlc.add(new FieldLabel(txtOldPassword, "Old password"), new VerticalLayoutContainer.VerticalLayoutData(1, -1));

        txtNewPassword = new PasswordField();
        vlc.add(txtNewPassword);
        vlc.add(new FieldLabel(txtNewPassword, "New password"), new VerticalLayoutContainer.VerticalLayoutData(1, -1));

        txtRepPassword = new PasswordField();
        vlc.add(new FieldLabel(txtRepPassword, "Repeat password"), new VerticalLayoutContainer.VerticalLayoutData(1, -1));

        setHeadingText("Change password");
        setWidth(400);
        setHideOnButtonClick(false);
        add(vlc);

        TextButton btnOk = new TextButton("Change Password");
        addButton(btnOk);

        btnOk.addSelectHandler(new SelectEvent.SelectHandler() {
            @Override
            public void onSelect(SelectEvent event) {
                doPasswordChange();
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

    private void doPasswordChange() {
        String oldPassword = txtOldPassword.getText();
        String newPassword = txtNewPassword.getText();
        String repPassword = txtRepPassword.getText();

        if (oldPassword == null || oldPassword.length() == 0) {
            AlertMessageBox amb = new AlertMessageBox("Password change", "You have to enter old password.");
            amb.show();
            return;
        }

        if (newPassword == null || newPassword.length() == 0 || repPassword == null || repPassword.length() == 0) {
            AlertMessageBox amb = new AlertMessageBox("Password change", "New password is empty.");
            amb.show();
            return;
        }

        if (!newPassword.equals(repPassword)) {
            txtNewPassword.setText("");
            txtRepPassword.setText("");
            AlertMessageBox amb = new AlertMessageBox("Invalid new password", "New passwords do not match.");
            amb.show();
            return;
        }

        userApi.resetPassword(oldPassword, newPassword, new MethodCallback<Void>() {
            @Override
            public void onFailure(Method method, Throwable e) {
                txtOldPassword.setText("");
                txtNewPassword.setText("");
                txtRepPassword.setText("");
                AlertMessageBox amb = new AlertMessageBox("Password change", "Password change failed: " + e.getMessage());
                amb.show();
            }

            @Override
            public void onSuccess(Method method, Void response) {
                txtOldPassword.setText("");
                txtNewPassword.setText("");
                txtRepPassword.setText("");

                AlertMessageBox amb = new AlertMessageBox("Password change", "Your password has been changed.");
                amb.setIcon(Resources.INSTANCE.msgBoxOkIcon());
                amb.show();
                hide();
            }
        });
    }

}
