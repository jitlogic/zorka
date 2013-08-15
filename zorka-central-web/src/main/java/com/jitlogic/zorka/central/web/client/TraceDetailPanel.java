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
package com.jitlogic.zorka.central.web.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.CellTree;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import com.jitlogic.zorka.central.db.DbRecord;


public class TraceDetailPanel extends Composite {
    interface TraceDetailPanelUiBinder extends UiBinder<Widget, TraceDetailPanel> { }

    private static TraceDetailPanelUiBinder ourUiBinder = GWT.create(TraceDetailPanelUiBinder.class);

    @UiField(provided = true) CellTree tree;
    @UiField Button btnClose;

    private ZorkaCentral central;

    private RoofRecord root;
    private RoofRecord trace;

    private TraceDetailViewModel model;
    private RoofClient<RoofRecord> client;


    public TraceDetailPanel(ZorkaCentral central, RoofClient<RoofRecord> client, RoofRecord trace, RoofRecord root) {
        this.central = central;
        this.client = client;

        this.trace = trace;
        this.root = root;

        CellTree.Resources res = GWT.create(CellTree.BasicResources.class);

        model = new TraceDetailViewModel(this.trace);
        tree = new CellTree(model, root, res);
        tree.setAnimationEnabled(false);


        initWidget(ourUiBinder.createAndBindUi(this));
    }


}

