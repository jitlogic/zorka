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
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.web.bindery.requestfactory.shared.Receiver;
import com.google.web.bindery.requestfactory.shared.ServerFailure;
import com.jitlogic.zico.client.ErrorHandler;
import com.jitlogic.zico.client.Resources;
import com.jitlogic.zico.client.inject.ZicoRequestFactory;
import com.jitlogic.zico.client.props.TraceTemplateInfoProperties;
import com.jitlogic.zico.shared.data.SymbolProxy;
import com.jitlogic.zico.shared.data.TraceTemplateProxy;
import com.jitlogic.zico.shared.services.SystemServiceProxy;
import com.sencha.gxt.core.client.Style;
import com.sencha.gxt.data.shared.LabelProvider;
import com.sencha.gxt.data.shared.ListStore;
import com.sencha.gxt.widget.core.client.button.TextButton;
import com.sencha.gxt.widget.core.client.container.VerticalLayoutContainer;
import com.sencha.gxt.widget.core.client.event.CompleteEditEvent;
import com.sencha.gxt.widget.core.client.event.SelectEvent;
import com.sencha.gxt.widget.core.client.form.NumberPropertyEditor;
import com.sencha.gxt.widget.core.client.form.SimpleComboBox;
import com.sencha.gxt.widget.core.client.form.SpinnerField;
import com.sencha.gxt.widget.core.client.form.TextField;
import com.sencha.gxt.widget.core.client.grid.ColumnConfig;
import com.sencha.gxt.widget.core.client.grid.ColumnModel;
import com.sencha.gxt.widget.core.client.grid.Grid;
import com.sencha.gxt.widget.core.client.grid.editing.ClicksToEdit;
import com.sencha.gxt.widget.core.client.grid.editing.GridRowEditing;
import com.sencha.gxt.widget.core.client.toolbar.SeparatorToolItem;
import com.sencha.gxt.widget.core.client.toolbar.ToolBar;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class TraceTemplatePanel extends VerticalLayoutContainer {

    private final static TraceTemplateInfoProperties props = GWT.create(TraceTemplateInfoProperties.class);

    private ListStore<TraceTemplateProxy> templateStore;
    private Grid<TraceTemplateProxy> templateGrid;
    private GridRowEditing<TraceTemplateProxy> templateEditor;

    private SpinnerField<Integer> txtOrder;
    private TextField txtCondition;
    private TextField txtTemplate;

    private ErrorHandler errorHandler;

    private ZicoRequestFactory rf;

    private SystemServiceProxy newTemplateRequest;

    @Inject
    public TraceTemplatePanel(ZicoRequestFactory rf, ErrorHandler errorHandler) {

        this.errorHandler = errorHandler;
        this.rf = rf;

        createToolbar();
        createTemplateListGrid();
        loadData();
    }


    private void createTemplateListGrid() {
        ColumnConfig<TraceTemplateProxy, Integer> orderCol
                = new ColumnConfig<TraceTemplateProxy, Integer>(props.order(), 80, "Order");
        orderCol.setFixed(true);

        final ColumnConfig<TraceTemplateProxy, String> traceConditionCol
                = new ColumnConfig<TraceTemplateProxy, String>(props.condition(), 250, "Condition Expression");

        ColumnConfig<TraceTemplateProxy, String> traceTemplateCol
                = new ColumnConfig<TraceTemplateProxy, String>(props.template(), 250, "Description Template");

        ColumnModel<TraceTemplateProxy> model = new ColumnModel<TraceTemplateProxy>(
                Arrays.<ColumnConfig<TraceTemplateProxy, ?>>asList(
                    orderCol, traceConditionCol, traceTemplateCol));

        templateStore = new ListStore<TraceTemplateProxy>(props.key());

        templateGrid = new Grid<TraceTemplateProxy>(templateStore, model);
        templateGrid.getSelectionModel().setSelectionMode(Style.SelectionMode.SINGLE);

        templateGrid.getView().setForceFit(true);
        templateGrid.getView().setAutoFill(true);


        templateEditor = new GridRowEditing<TraceTemplateProxy>(templateGrid);
        templateEditor.setClicksToEdit(ClicksToEdit.TWO);

        txtOrder = new SpinnerField<Integer>(new NumberPropertyEditor.IntegerPropertyEditor());
        txtOrder.setMinValue(0);
        txtOrder.setMaxValue(9999999);
        templateEditor.addEditor(orderCol, txtOrder);

        txtCondition = new TextField();
        templateEditor.addEditor(traceConditionCol, txtCondition);

        txtTemplate = new TextField();
        templateEditor.addEditor(traceTemplateCol, txtTemplate);


        templateEditor.addCompleteEditHandler(new CompleteEditEvent.CompleteEditHandler<TraceTemplateProxy>() {
            @Override
            public void onCompleteEdit(final CompleteEditEvent<TraceTemplateProxy> event) {
                saveChanges(event.getEditCell().getRow());
            }
        });

        add(templateGrid, new VerticalLayoutData(1, 1));
    }


    private void saveChanges(int row) {
        SystemServiceProxy req = newTemplateRequest != null ? newTemplateRequest : rf.systemService();
        newTemplateRequest = null;
        final TraceTemplateProxy tti = req.edit(templateStore.get(row));
        tti.setOrder(txtOrder.getCurrentValue());
        tti.setCondition(txtCondition.getCurrentValue());
        tti.setTemplate(txtTemplate.getCurrentValue());
        req.saveTemplate(tti).fire(new Receiver<Integer>() {
            @Override
            public void onSuccess(Integer integer) {
                loadData();
            }
            @Override
            public void onFailure(ServerFailure failure) {
                errorHandler.error("Error saving template", failure);
            }
        });
    }


    private void createToolbar() {
        ToolBar toolBar = new ToolBar();

        TextButton btnRefresh = new TextButton();
        btnRefresh.setIcon(Resources.INSTANCE.refreshIcon());
        btnRefresh.setToolTip("Refresh list");

        toolBar.add(btnRefresh);

        btnRefresh.addSelectHandler(new SelectEvent.SelectHandler() {
            @Override
            public void onSelect(SelectEvent event) {
                loadData();
            }
        });

        toolBar.add(new SeparatorToolItem());

        TextButton btnNew = new TextButton();
        btnNew.setIcon(Resources.INSTANCE.addIcon());
        btnNew.setToolTip("Add new template");

        toolBar.add(btnNew);

        btnNew.addSelectHandler(new SelectEvent.SelectHandler() {
            @Override
            public void onSelect(SelectEvent event) {
                addTemplate();
            }
        });

        TextButton btnRemove = new TextButton();
        btnRemove.setIcon(Resources.INSTANCE.removeIcon());
        btnRemove.setToolTip("Remove template");

        toolBar.add(btnRemove);

        btnRemove.addSelectHandler(new SelectEvent.SelectHandler() {
            @Override
            public void onSelect(SelectEvent event) {
                removeSelectedTemplate();
            }
        });

        toolBar.add(new SeparatorToolItem());

        TextButton btnEdit = new TextButton();
        btnEdit.setIcon(Resources.INSTANCE.editIcon());
        btnEdit.setToolTip("Modify template");

        toolBar.add(btnEdit);

        btnEdit.addSelectHandler(new SelectEvent.SelectHandler() {
            @Override
            public void onSelect(SelectEvent event) {
                editTemplate();
            }
        });

        add(toolBar, new VerticalLayoutData(1, -1));
    }

    private void editTemplate() {
        TraceTemplateProxy tti = templateGrid.getSelectionModel().getSelectedItem();
        if (tti != null) {
            int row = templateStore.indexOf(tti);
            templateEditor.startEditing(new Grid.GridCell(row, 2));
        }
    }

    private void addTemplate() {
        newTemplateRequest = rf.systemService();
        templateStore.add(0, newTemplateRequest.create(TraceTemplateProxy.class));
        templateEditor.startEditing(new Grid.GridCell(0, 2));
    }


    private void removeSelectedTemplate() {
        TraceTemplateProxy template = templateGrid.getSelectionModel().getSelectedItem();
        if (template != null) {
            templateStore.remove(template);
            rf.systemService().removeTemplate(template.getId()).fire();
        }
    }


    private void loadData() {
        rf.systemService().listTemplates().fire(new Receiver<List<TraceTemplateProxy>>() {
            @Override
            public void onSuccess(List<TraceTemplateProxy> response) {
                updateData(response);
            }

            @Override
            public void onFailure(ServerFailure e) {
                errorHandler.error("Error loading trace templates: ", e);
            }
        });
    }


    private void updateData(List<TraceTemplateProxy> data) {
        Collections.sort(data, new Comparator<TraceTemplateProxy>() {
            @Override
            public int compare(TraceTemplateProxy o1, TraceTemplateProxy o2) {
                return o1.getOrder() - o2.getOrder();
            }
        });
        templateStore.clear();
        templateStore.addAll(data);
    }
}
