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
import com.jitlogic.zico.data.TraceTemplateInfoProperties;
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

import java.util.*;


public class TraceTemplatePanel extends VerticalLayoutContainer {

    private final static TraceTemplateInfoProperties props = GWT.create(TraceTemplateInfoProperties.class);

    private ListStore<TraceTemplateProxy> templateStore;
    private Grid<TraceTemplateProxy> templateGrid;
    private GridRowEditing<TraceTemplateProxy> templateEditor;

    private SimpleComboBox<Integer> cmbTraceType;

    private Map<String, Integer> ttidByName = new HashMap<String, Integer>();
    private Map<Integer, String> ttidByType = new HashMap<Integer, String>();

    private SpinnerField<Integer> txtOrder;
    private TextField txtCondTempl;
    private TextField txtCondRegex;
    private TextField txtTraceTemplate;

    private ErrorHandler errorHandler;

    private ZicoRequestFactory rf;

    private SystemServiceProxy newTemplateRequest;

    @Inject
    public TraceTemplatePanel(ZicoRequestFactory rf, ErrorHandler errorHandler,
                              @Assisted Map<String, String> tidMap) {

        this.errorHandler = errorHandler;
        this.rf = rf;

        createToolbar();
        createTemplateListGrid();
        loadTids(tidMap);
        loadData();
    }


    private void createTemplateListGrid() {
        ColumnConfig<TraceTemplateProxy, Integer> traceIdCol
                = new ColumnConfig<TraceTemplateProxy, Integer>(props.traceId(), 120, "Type");
        traceIdCol.setAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        traceIdCol.setFixed(true);

        traceIdCol.setCell(new AbstractCell<Integer>() {
            @Override
            public void render(Context context, Integer value, SafeHtmlBuilder sb) {
                String s = traceTypeName(value);
                sb.append(SafeHtmlUtils.fromString(s));
            }
        });

        ColumnConfig<TraceTemplateProxy, Integer> orderCol
                = new ColumnConfig<TraceTemplateProxy, Integer>(props.order(), 80, "Order");
        orderCol.setFixed(true);

        final ColumnConfig<TraceTemplateProxy, String> traceCondTemplCol
                = new ColumnConfig<TraceTemplateProxy, String>(props.condTemplate(), 250, "Condition Template");

        ColumnConfig<TraceTemplateProxy, String> traceCondRegexCol
                = new ColumnConfig<TraceTemplateProxy, String>(props.condRegex(), 250, "Condition Match");

        ColumnConfig<TraceTemplateProxy, String> traceTemplateCol
                = new ColumnConfig<TraceTemplateProxy, String>(props.template(), 250, "Description Template");

        ColumnModel<TraceTemplateProxy> model = new ColumnModel<TraceTemplateProxy>(
                Arrays.<ColumnConfig<TraceTemplateProxy, ?>>asList(
                    traceIdCol, orderCol, traceCondTemplCol, traceCondRegexCol, traceTemplateCol));

        templateStore = new ListStore<TraceTemplateProxy>(props.key());

        templateGrid = new Grid<TraceTemplateProxy>(templateStore, model);
        templateGrid.getSelectionModel().setSelectionMode(Style.SelectionMode.SINGLE);

        templateGrid.getView().setForceFit(true);
        templateGrid.getView().setAutoFill(true);


        templateEditor = new GridRowEditing<TraceTemplateProxy>(templateGrid);
        templateEditor.setClicksToEdit(ClicksToEdit.TWO);

        cmbTraceType = new SimpleComboBox<Integer>(new LabelProvider<Integer>() {
            @Override
            public String getLabel(Integer item) {
                return ttidByType.get(item);
            }
        });
        cmbTraceType.setForceSelection(true);

        templateEditor.addEditor(traceIdCol, cmbTraceType);

        txtOrder = new SpinnerField<Integer>(new NumberPropertyEditor.IntegerPropertyEditor());
        txtOrder.setMinValue(0);
        txtOrder.setMaxValue(9999999);
        templateEditor.addEditor(orderCol, txtOrder);

        txtCondTempl = new TextField();
        templateEditor.addEditor(traceCondTemplCol, txtCondTempl);

        txtCondRegex = new TextField();
        templateEditor.addEditor(traceCondRegexCol, txtCondRegex);

        txtTraceTemplate = new TextField();
        templateEditor.addEditor(traceTemplateCol, txtTraceTemplate);


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
        tti.setTraceId(cmbTraceType.getCurrentValue());
        tti.setOrder(txtOrder.getCurrentValue());
        tti.setCondTemplate(txtCondTempl.getCurrentValue());
        tti.setCondRegex(txtCondRegex.getCurrentValue());
        tti.setTemplate(txtTraceTemplate.getCurrentValue());
        req.saveTemplate(tti).fire(new Receiver<Integer>() {
            @Override
            public void onSuccess(Integer integer) {
                loadData();
            }
        });
    }


    private String traceTypeName(Integer value) {
        return ttidByType.containsKey(value) ? ttidByType.get(value) : "" + value;
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


    private void loadTids(Map<String, String> tidMap) {
        for (Map.Entry<String, String> e : tidMap.entrySet()) {
            // TODO this is a crutch; bug in RestyGWT/Jersey or use beans instead ?
            int tid = Integer.parseInt(e.getKey());
            String name = e.getValue();
            ttidByName.put(name, tid);
            ttidByType.put(tid, name);
            cmbTraceType.add(tid);
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
                if (o1.getTraceId() != o2.getTraceId()) {
                    return traceTypeName(o1.getTraceId()).compareTo(traceTypeName(o2.getTraceId()));
                }
                return o1.getOrder() - o2.getOrder();
            }
        });
        templateStore.clear();
        templateStore.addAll(data);
    }
}
