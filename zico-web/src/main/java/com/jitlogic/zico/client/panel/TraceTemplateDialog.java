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


import com.google.web.bindery.requestfactory.shared.Receiver;
import com.google.web.bindery.requestfactory.shared.ServerFailure;
import com.jitlogic.zico.client.ErrorHandler;
import com.jitlogic.zico.client.inject.ZicoRequestFactory;
import com.jitlogic.zico.shared.data.TraceTemplateProxy;
import com.jitlogic.zico.shared.services.SystemServiceProxy;
import com.sencha.gxt.widget.core.client.Dialog;
import com.sencha.gxt.widget.core.client.button.TextButton;
import com.sencha.gxt.widget.core.client.container.VerticalLayoutContainer;
import com.sencha.gxt.widget.core.client.event.SelectEvent;
import com.sencha.gxt.widget.core.client.form.FieldLabel;
import com.sencha.gxt.widget.core.client.form.NumberPropertyEditor;
import com.sencha.gxt.widget.core.client.form.SpinnerField;
import com.sencha.gxt.widget.core.client.form.TextField;

public class TraceTemplateDialog extends Dialog {

    private SystemServiceProxy editTemplateRequest;

    private TraceTemplateProxy editedTemplate;

    private ErrorHandler errorHandler;

    private TraceTemplatePanel panel;

    private SpinnerField<Integer> txtOrder;
    private TextField txtCondition;
    private TextField txtTemplate;

    public TraceTemplateDialog(ZicoRequestFactory rf, TraceTemplatePanel panel, TraceTemplateProxy tti, ErrorHandler errorHandler) {
        this.panel = panel;
        this.errorHandler = errorHandler;

        editTemplateRequest = rf.systemService();
        editedTemplate = tti != null
                ? editTemplateRequest.edit(tti)
                : editTemplateRequest.create(TraceTemplateProxy.class);

        setHeadingText(tti != null ? "Editing template" : "New template");

        createUi(tti);
    }

    private void createUi(TraceTemplateProxy tti) {
        setPredefinedButtons();

        VerticalLayoutContainer vlc = new VerticalLayoutContainer();

        txtOrder = new SpinnerField<Integer>(new NumberPropertyEditor.IntegerPropertyEditor());
        vlc.add(txtOrder);
        vlc.add(new FieldLabel(txtOrder, "Order"), new VerticalLayoutContainer.VerticalLayoutData(1, -1));
        txtOrder.setValue(tti != null ? tti.getOrder() : 100);

        txtCondition = new TextField();
        vlc.add(txtCondition);
        vlc.add(new FieldLabel(txtCondition, "Condition"), new VerticalLayoutContainer.VerticalLayoutData(1, -1));
        txtCondition.setValue(tti != null ? tti.getCondition() : "trace = 'MY_TRACE' and MY_FIELD != null");

        txtTemplate = new TextField();
        vlc.add(txtTemplate);
        vlc.add(new FieldLabel(txtTemplate, "Template"), new VerticalLayoutContainer.VerticalLayoutData(1, -1));
        txtTemplate.setValue(tti != null ? tti.getTemplate() : "MY_TRACE: ${MY_FIELD}");

        setWidth(640);
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
        editedTemplate.setOrder(txtOrder.getCurrentValue());
        editedTemplate.setCondition(txtCondition.getText());
        editedTemplate.setTemplate(txtTemplate.getText());

        editTemplateRequest.saveTemplate(editedTemplate).fire(new Receiver<Integer>() {
            @Override
            public void onSuccess(Integer response) {
                hide();
                panel.refreshTemplates();
            }
            @Override
            public void onFailure(ServerFailure failure) {
                errorHandler.error("Error saving trace template", failure);
            }
        });
    }
}
