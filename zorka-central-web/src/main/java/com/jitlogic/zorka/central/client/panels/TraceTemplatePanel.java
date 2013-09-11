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
package com.jitlogic.zorka.central.client.panels;

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.jitlogic.zorka.central.client.Resources;
import com.jitlogic.zorka.central.client.api.AdminApi;
import com.jitlogic.zorka.central.data.TraceTemplateInfo;
import com.jitlogic.zorka.central.data.TraceTemplateInfoProperties;
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
import org.fusesource.restygwt.client.Method;
import org.fusesource.restygwt.client.MethodCallback;

import java.util.*;


public class TraceTemplatePanel extends VerticalLayoutContainer {

    private final static TraceTemplateInfoProperties props = GWT.create(TraceTemplateInfoProperties.class);

    private AdminApi adminService;
    private ListStore<TraceTemplateInfo> templateStore;
    private Grid<TraceTemplateInfo> templateGrid;
    private GridRowEditing<TraceTemplateInfo> templateEditor;

    private SimpleComboBox<Integer> cmbTraceType;

    private Map<String, Integer> ttidByName = new HashMap<String, Integer>();
    private Map<Integer, String> ttidByType = new HashMap<Integer, String>();
    private SpinnerField<Integer> txtOrder;
    private TextField txtCondTempl;
    private TextField txtCondRegex;
    private TextField txtTraceTemplate;

    @Inject
    public TraceTemplatePanel(AdminApi adminService, @Assisted Map<String, String> tidMap) {
        this.adminService = adminService;

        createToolbar();
        createTemplateListGrid();
        loadTids(tidMap);
        loadData();
    }


    private void createTemplateListGrid() {
        ColumnConfig<TraceTemplateInfo, Integer> traceIdCol
                = new ColumnConfig<TraceTemplateInfo, Integer>(props.traceId(), 120, "Type");
        traceIdCol.setAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        traceIdCol.setFixed(true);

        traceIdCol.setCell(new AbstractCell<Integer>() {
            @Override
            public void render(Context context, Integer value, SafeHtmlBuilder sb) {
                String s = traceTypeName(value);
                sb.append(SafeHtmlUtils.fromString(s));
            }
        });

        ColumnConfig<TraceTemplateInfo, Integer> orderCol
                = new ColumnConfig<TraceTemplateInfo, Integer>(props.order(), 80, "Order");
        orderCol.setFixed(true);

        final ColumnConfig<TraceTemplateInfo, String> traceCondTemplCol
                = new ColumnConfig<TraceTemplateInfo, String>(props.condTemplate(), 250, "Condition Template");

        ColumnConfig<TraceTemplateInfo, String> traceCondRegexCol
                = new ColumnConfig<TraceTemplateInfo, String>(props.condRegex(), 250, "Condition Match");

        ColumnConfig<TraceTemplateInfo, String> traceTemplateCol
                = new ColumnConfig<TraceTemplateInfo, String>(props.template(), 250, "Description Template");

        ColumnModel<TraceTemplateInfo> model = new ColumnModel<TraceTemplateInfo>(Arrays.<ColumnConfig<TraceTemplateInfo, ?>>asList(
                traceIdCol, orderCol, traceCondTemplCol, traceCondRegexCol, traceTemplateCol));

        templateStore = new ListStore<TraceTemplateInfo>(props.key());

        templateGrid = new Grid<TraceTemplateInfo>(templateStore, model);
        templateGrid.getSelectionModel().setSelectionMode(Style.SelectionMode.SINGLE);

        templateGrid.getView().setForceFit(true);
        templateGrid.getView().setAutoFill(true);


        templateEditor = new GridRowEditing<TraceTemplateInfo>(templateGrid);
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


        templateEditor.addCompleteEditHandler(new CompleteEditEvent.CompleteEditHandler<TraceTemplateInfo>() {
            @Override
            public void onCompleteEdit(final CompleteEditEvent<TraceTemplateInfo> event) {
                saveChanges(event.getEditCell().getRow());
            }
        });

        add(templateGrid, new VerticalLayoutData(1, 1));
    }


    private void saveChanges(int row) {
        final TraceTemplateInfo tti = templateStore.get(row);
        tti.setTraceId(cmbTraceType.getCurrentValue());
        tti.setOrder(txtOrder.getCurrentValue());
        tti.setCondTemplate(txtCondTempl.getCurrentValue());
        tti.setCondRegex(txtCondRegex.getCurrentValue());
        tti.setTemplate(txtTraceTemplate.getCurrentValue());
        adminService.saveTemplate(tti, new MethodCallback<Integer>() {
            @Override
            public void onFailure(Method method, Throwable exception) {
                GWT.log("Error calling method " + method, exception);
            }

            @Override
            public void onSuccess(Method method, Integer response) {
                if (tti.getId() == 0) {
                    tti.setId(response);
                }
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
                templateStore.add(0, new TraceTemplateInfo());
                templateEditor.startEditing(new Grid.GridCell(0, 2));
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
                TraceTemplateInfo tti = templateGrid.getSelectionModel().getSelectedItem();
                if (tti != null) {
                    int row = templateStore.indexOf(tti);
                    templateEditor.startEditing(new Grid.GridCell(row, 2));
                }
            }
        });

        add(toolBar, new VerticalLayoutData(1, -1));
    }


    private void removeSelectedTemplate() {
        TraceTemplateInfo tti = templateGrid.getSelectionModel().getSelectedItem();
        if (tti != null) {
            adminService.removeTemplate(tti.getId(), new MethodCallback<Void>() {
                @Override
                public void onFailure(Method method, Throwable exception) {
                    GWT.log("Error executing method " + method, exception);
                }

                @Override
                public void onSuccess(Method method, Void response) {
                    loadData();
                }
            });
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
        adminService.listTemplates(new MethodCallback<List<TraceTemplateInfo>>() {
            @Override
            public void onFailure(Method method, Throwable exception) {
                GWT.log("Error calling " + method, exception);
            }

            @Override
            public void onSuccess(Method method, List<TraceTemplateInfo> response) {
                updateData(response);
            }
        });
    }


    private void updateData(List<TraceTemplateInfo> data) {
        Collections.sort(data, new Comparator<TraceTemplateInfo>() {
            @Override
            public int compare(TraceTemplateInfo o1, TraceTemplateInfo o2) {
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
