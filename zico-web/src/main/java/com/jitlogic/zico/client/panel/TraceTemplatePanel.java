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
import com.google.gwt.cell.client.Cell;
import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Style;
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
import com.google.gwt.view.client.CellPreviewEvent;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SingleSelectionModel;
import com.google.inject.Inject;
import com.google.web.bindery.requestfactory.shared.Receiver;
import com.google.web.bindery.requestfactory.shared.ServerFailure;
import com.jitlogic.zico.client.ErrorHandler;
import com.jitlogic.zico.client.Resources;
import com.jitlogic.zico.client.inject.ZicoRequestFactory;
import com.jitlogic.zico.shared.data.TraceTemplateProxy;
import com.sencha.gxt.widget.core.client.button.TextButton;
import com.sencha.gxt.widget.core.client.container.VerticalLayoutContainer;
import com.sencha.gxt.widget.core.client.event.SelectEvent;
import com.sencha.gxt.widget.core.client.menu.Item;
import com.sencha.gxt.widget.core.client.menu.Menu;
import com.sencha.gxt.widget.core.client.menu.MenuItem;
import com.sencha.gxt.widget.core.client.menu.SeparatorMenuItem;
import com.sencha.gxt.widget.core.client.toolbar.SeparatorToolItem;
import com.sencha.gxt.widget.core.client.toolbar.ToolBar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


public class TraceTemplatePanel extends VerticalLayoutContainer {

    private ListDataProvider<TraceTemplateProxy> templateStore;
    private DataGrid<TraceTemplateProxy> templateGrid;
    private SingleSelectionModel<TraceTemplateProxy> selectionModel;

    private ErrorHandler errorHandler;
    private ZicoRequestFactory rf;

    private Menu contextMenu;


    @Inject
    public TraceTemplatePanel(ZicoRequestFactory rf, ErrorHandler errorHandler) {

        this.errorHandler = errorHandler;
        this.rf = rf;

        createToolbar();
        createTemplateListGrid();
        createContextMenu();

        refreshTemplates();
    }

    private final static ProvidesKey<TraceTemplateProxy> KEY_PROVIDER = new ProvidesKey<TraceTemplateProxy>() {
        @Override
        public Object getKey(TraceTemplateProxy item) {
            return item.getId();
        }
    };

    private final static Cell<TraceTemplateProxy> ORDER_CELL = new AbstractCell<TraceTemplateProxy>() {
        @Override
        public void render(Context context, TraceTemplateProxy value, SafeHtmlBuilder sb) {
            sb.append(SafeHtmlUtils.fromString(""+value.getOrder()));
        }
    };

    private final static Cell<TraceTemplateProxy> CONDITION_CELL = new AbstractCell<TraceTemplateProxy>() {
        @Override
        public void render(Context context, TraceTemplateProxy value, SafeHtmlBuilder sb) {
            sb.append(SafeHtmlUtils.fromString(""+value.getCondition()));
        }
    };

    private final static Cell<TraceTemplateProxy> TEMPLATE_CELL = new AbstractCell<TraceTemplateProxy>() {
        @Override
        public void render(Context context, TraceTemplateProxy value, SafeHtmlBuilder sb) {
            sb.append(SafeHtmlUtils.fromString(""+value.getTemplate()));
        }
    };


    private void createTemplateListGrid() {
        templateGrid = new DataGrid<TraceTemplateProxy>(1024*1024, KEY_PROVIDER);
        selectionModel = new SingleSelectionModel<TraceTemplateProxy>(KEY_PROVIDER);
        templateGrid.setSelectionModel(selectionModel);

        Column<TraceTemplateProxy,TraceTemplateProxy> colOrder = new IdentityColumn<TraceTemplateProxy>(ORDER_CELL);
        templateGrid.addColumn(colOrder, new ResizableHeader<TraceTemplateProxy>("Order", templateGrid, colOrder));
        templateGrid.setColumnWidth(colOrder, 80, Style.Unit.PX);

        Column<TraceTemplateProxy,TraceTemplateProxy> colCondition = new IdentityColumn<TraceTemplateProxy>(CONDITION_CELL);
        templateGrid.addColumn(colCondition, new ResizableHeader<TraceTemplateProxy>("Condition", templateGrid, colCondition));
        templateGrid.setColumnWidth(colCondition, 250, Style.Unit.PX);

        Column<TraceTemplateProxy,TraceTemplateProxy> colTemplate = new IdentityColumn<TraceTemplateProxy>(TEMPLATE_CELL);
        templateGrid.addColumn(colTemplate, "Description Template");
        templateGrid.setColumnWidth(colTemplate, 100, Style.Unit.PCT);

        templateStore = new ListDataProvider<TraceTemplateProxy>(KEY_PROVIDER);
        templateStore.addDataDisplay(templateGrid);

        templateGrid.addCellPreviewHandler(new CellPreviewEvent.Handler<TraceTemplateProxy>() {
            @Override
            public void onCellPreview(CellPreviewEvent<TraceTemplateProxy> event) {
                NativeEvent nev = event.getNativeEvent();
                String eventType = nev.getType();
                if ((BrowserEvents.KEYDOWN.equals(eventType) && nev.getKeyCode() == KeyCodes.KEY_ENTER)
                        || BrowserEvents.DBLCLICK.equals(nev.getType())) {
                    selectionModel.setSelected(event.getValue(), true);
                    editTemplate();
                }
                if (BrowserEvents.CONTEXTMENU.equals(eventType)) {
                    selectionModel.setSelected(event.getValue(), true);
                    if (event.getValue() != null) {
                        contextMenu.showAt(event.getNativeEvent().getClientX(), event.getNativeEvent().getClientY());
                    }
                }

            }
        });

        templateGrid.addDomHandler(new DoubleClickHandler() {
            @Override
            public void onDoubleClick(DoubleClickEvent event) {
                event.preventDefault();
            }
        }, DoubleClickEvent.getType());

        templateGrid.addDomHandler(new ContextMenuHandler() {
            @Override
            public void onContextMenu(ContextMenuEvent event) {
                event.preventDefault();
            }
        }, ContextMenuEvent.getType());

        add(templateGrid, new VerticalLayoutData(1, 1));
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
                refreshTemplates();
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
                removeTemplate();
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


    private void createContextMenu() {
        contextMenu = new Menu();

        MenuItem mnuRefresh = new MenuItem("Refresh");
        mnuRefresh.setIcon(Resources.INSTANCE.refreshIcon());
        mnuRefresh.addSelectionHandler(new SelectionHandler<Item>() {
            @Override
            public void onSelection(SelectionEvent<Item> event) {
                refreshTemplates();
            }
        });
        contextMenu.add(mnuRefresh);

        contextMenu.add(new SeparatorMenuItem());

        MenuItem mnuCreateTemplate = new MenuItem("New template");
        mnuCreateTemplate.setIcon(Resources.INSTANCE.addIcon());
        mnuCreateTemplate.addSelectionHandler(new SelectionHandler<Item>() {
            @Override
            public void onSelection(SelectionEvent<Item> event) {
                addTemplate();
            }
        });
        contextMenu.add(mnuCreateTemplate);


        MenuItem mnuRemoveTemplate = new MenuItem("Remove template");
        mnuRemoveTemplate.setIcon(Resources.INSTANCE.removeIcon());
        mnuRemoveTemplate.addSelectionHandler(new SelectionHandler<Item>() {
            @Override
            public void onSelection(SelectionEvent<Item> event) {
                removeTemplate();
            }
        });
        contextMenu.add(mnuRemoveTemplate);

        contextMenu.add(new SeparatorMenuItem());

        MenuItem mnuEditTemplate = new MenuItem("Edit Template");
        mnuEditTemplate.setIcon(Resources.INSTANCE.editIcon());
        mnuEditTemplate.addSelectionHandler(new SelectionHandler<Item>() {
            @Override
            public void onSelection(SelectionEvent<Item> event) {
                editTemplate();
            }
        });

        contextMenu.add(mnuEditTemplate);
    }


    private void editTemplate() {
        TraceTemplateProxy tti = selectionModel.getSelectedObject();
        if (tti != null) {
            new TraceTemplateDialog(rf, this, tti, errorHandler).show();
        }
    }


    private void addTemplate() {
        new TraceTemplateDialog(rf, this, null, errorHandler).show();
    }


    private void removeTemplate() {
        TraceTemplateProxy template = selectionModel.getSelectedObject();
        if (template != null) {
            rf.systemService().removeTemplate(template.getId()).fire(
                    new Receiver<Void>() {
                        @Override
                        public void onSuccess(Void response) {
                            refreshTemplates();
                        }
                    }
            );
        }
    }


    public void refreshTemplates() {
        rf.systemService().listTemplates().fire(new Receiver<List<TraceTemplateProxy>>() {
            @Override
            public void onSuccess(List<TraceTemplateProxy> response) {
                Collections.sort(response, new Comparator<TraceTemplateProxy>() {
                    @Override
                    public int compare(TraceTemplateProxy o1, TraceTemplateProxy o2) {
                        return o1.getOrder() - o2.getOrder();
                    }
                });
                templateStore.getList().clear();
                templateStore.getList().addAll(response);
            }

            @Override
            public void onFailure(ServerFailure e) {
                errorHandler.error("Error loading trace templates: ", e);
            }
        });
    }


}
