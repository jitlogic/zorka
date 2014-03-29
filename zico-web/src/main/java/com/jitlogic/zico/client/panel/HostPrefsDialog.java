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


import com.google.web.bindery.requestfactory.shared.Receiver;
import com.google.web.bindery.requestfactory.shared.Request;
import com.google.web.bindery.requestfactory.shared.ServerFailure;
import com.jitlogic.zico.client.ErrorHandler;
import com.jitlogic.zico.client.inject.ZicoRequestFactory;
import com.jitlogic.zico.shared.data.HostProxy;
import com.jitlogic.zico.shared.services.HostServiceProxy;
import com.sencha.gxt.widget.core.client.Dialog;
import com.sencha.gxt.widget.core.client.button.TextButton;
import com.sencha.gxt.widget.core.client.container.VerticalLayoutContainer;
import com.sencha.gxt.widget.core.client.event.SelectEvent;
import com.sencha.gxt.widget.core.client.form.FieldLabel;
import com.sencha.gxt.widget.core.client.form.NumberPropertyEditor;
import com.sencha.gxt.widget.core.client.form.SpinnerField;
import com.sencha.gxt.widget.core.client.form.TextField;

public class HostPrefsDialog extends Dialog {

    private HostProxy editedHost;
    private HostServiceProxy editHostRequest;

    private HostListPanel panel;

    private TextField txtHostName;
    private TextField txtHostAddr;
    private TextField txtHostGroup;
    private TextField txtHostDesc;
    private TextField txtHostPass;

    private SpinnerField<Long> txtMaxSize;

    private ErrorHandler errorHandler;

    private static final long KB = 1024;
    private static final long MB = 1024*KB;
    private static final long GB = 1024*MB;

    public HostPrefsDialog(ZicoRequestFactory rf, HostListPanel panel, HostProxy info, ErrorHandler errorHandler) {
        this.panel = panel;
        this.errorHandler = errorHandler;

        editHostRequest = rf.hostService();
        if (info != null) {
            editedHost = editHostRequest.edit(info);
        }

        setHeadingText(info != null ? "Edit host: " + info.getName() : "New host");
        setPredefinedButtons();

        createUi(info);
    }


    private void createUi(HostProxy info) {
        VerticalLayoutContainer vlc = new VerticalLayoutContainer();

        if (info == null) {
            txtHostName = new TextField();
            txtHostName.setAllowBlank(false);
            vlc.add(txtHostName);
            vlc.add(new FieldLabel(txtHostName, "Host name"),
                    new VerticalLayoutContainer.VerticalLayoutData(1, -1));
        }

        txtHostGroup = new TextField();
        vlc.add(txtHostGroup);
        vlc.add(new FieldLabel(txtHostGroup, "Host group"),
                new VerticalLayoutContainer.VerticalLayoutData(1, -1));

        if (info != null) {
            txtHostGroup.setText(info.getGroup());
        }

        txtHostAddr = new TextField();
        vlc.add(txtHostAddr);
        vlc.add(new FieldLabel(txtHostAddr, "Host address"),
                new VerticalLayoutContainer.VerticalLayoutData(1, -1));

        if (info != null) {
            txtHostAddr.setText(info.getAddr());
        }


        txtHostPass = new TextField();
        vlc.add(txtHostPass);
        vlc.add(new FieldLabel(txtHostPass, "Passphrase"),
                new VerticalLayoutContainer.VerticalLayoutData(1, -1));

        if (info != null) {
            txtHostPass.setText(info.getPass());
        }


        txtMaxSize = new SpinnerField<Long>(new NumberPropertyEditor.LongPropertyEditor());
        txtMaxSize.setIncrement(1L);
        txtMaxSize.setMinValue(1);
        txtMaxSize.setMaxValue(10240L);
        txtMaxSize.setAllowBlank(false);
        txtMaxSize.setToolTip("Maximum amount of trace data stored for this host.");
        vlc.add(txtMaxSize);
        vlc.add(new FieldLabel(txtMaxSize, "Store size (GB)"),
                new VerticalLayoutContainer.VerticalLayoutData(1, -1));

        long sz = info != null ? info.getMaxSize() / GB : 1;

        if (sz < 1) { sz = 1; }

        if (info != null) {
            txtMaxSize.setText("" + sz);
        } else {
            txtMaxSize.setText("1");
        }

        txtHostDesc = new TextField();
        vlc.add(txtHostDesc);
        vlc.add(new FieldLabel(txtHostDesc, "Comment"),
                new VerticalLayoutContainer.VerticalLayoutData(1, -1));

        if (info != null) {
            txtHostDesc.setText(info.getComment());
        }

        setWidth(400);
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


    public void save() {

        Request<Void> req;

        if (editedHost == null) {

            String name = txtHostName.getText();
            if (name.length() == 0 || !name.matches("^[0-9a-zA-Z_\\-\\.]+$")) {
                errorHandler.error("Illegal host name: '" + name + "'. Use only those characters: 0-9a-zA-Z_-");
                return;
            }

            req = editHostRequest.newHost(
                name,
                txtHostAddr.getText(),
                txtHostGroup.getText(),
                txtHostDesc.getText(),
                txtHostPass.getText(),
                txtMaxSize.getCurrentValue() * GB);
        } else {
            editedHost.setAddr(txtHostAddr.getText());
            editedHost.setGroup(txtHostGroup.getText());
            editedHost.setComment(txtHostDesc.getText());
            editedHost.setPass(txtHostPass.getText());
            editedHost.setMaxSize(txtMaxSize.getCurrentValue() * GB);
            req = editHostRequest.persist(editedHost);
        }

        req.fire(new Receiver<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                hide();
                panel.refresh();
            }
            @Override
            public void onFailure(ServerFailure error) {
                errorHandler.error("Cannot save host settings", error);
            }
        });
    }
}
