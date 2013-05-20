package com.jitlogic.zorka.console.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

public class TracePanel extends Composite {
    interface TracePanelUiBinder extends UiBinder<Widget, TracePanel> { }

    private static TracePanelUiBinder ourUiBinder = GWT.create(TracePanelUiBinder.class);

    @UiField CellTable<String[]> cellTable;

    public TracePanel() {
        initWidget(ourUiBinder.createAndBindUi(this));

    }
}