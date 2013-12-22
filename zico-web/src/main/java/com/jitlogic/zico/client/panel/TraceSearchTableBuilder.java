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


import com.google.gwt.cell.client.Cell;
import com.google.gwt.dom.builder.shared.DivBuilder;
import com.google.gwt.dom.builder.shared.TableCellBuilder;
import com.google.gwt.dom.builder.shared.TableRowBuilder;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.cellview.client.AbstractCellTable;
import com.google.gwt.user.cellview.client.AbstractCellTableBuilder;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.IdentityColumn;
import com.google.gwt.user.cellview.client.RowStyles;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.view.client.SelectionModel;
import com.jitlogic.zico.client.Resources;
import com.jitlogic.zico.shared.data.TraceInfoProxy;
import com.jitlogic.zico.shared.data.TraceRecordProxy;

import java.util.Set;

public class TraceSearchTableBuilder extends AbstractCellTableBuilder<TraceInfoProxy> {

    private final String evenRowStyle;
    private final String selectedRowStyle;
    private final String cellStyle;
    private final String evenCellStyle;
    private final String firstColumnStyle;
    private final String lastColumnStyle;
    private final String selectedCellStyle;
    private final String extenderCellStyle;


    private Set<Long> expandedDetails;
    private Column<TraceInfoProxy,TraceInfoProxy> colDetails;

    /**
     * Construct a new table builder.
     *
     * @param cellTable the table this builder will build rows for
     */
    public TraceSearchTableBuilder(AbstractCellTable<TraceInfoProxy> cellTable,
                                   Set<Long> expandedDetails) {
        super(cellTable);

        // Cache styles for faster access.
        AbstractCellTable.Style style = cellTable.getResources().style();
        evenRowStyle = style.evenRow();
        selectedRowStyle = " " + style.selectedRow();
        cellStyle = style.cell();
        evenCellStyle = " " + style.evenRowCell();
        firstColumnStyle = " " + style.firstColumn();
        lastColumnStyle = " " + style.lastColumn();
        selectedCellStyle = " " + style.selectedRowCell();
        extenderCellStyle = Resources.INSTANCE.zicoCssResources().methodDetailCell();

        this.colDetails = new IdentityColumn<TraceInfoProxy>(new TraceDetailCell());
        this.expandedDetails = expandedDetails;
    }


    @Override
    protected void buildRowImpl(TraceInfoProxy rowValue, int absRowIndex) {
        // Calculate the row styles.
        SelectionModel<? super TraceInfoProxy> selectionModel = cellTable.getSelectionModel();
        boolean isSelected = (selectionModel == null || rowValue == null) ? false : selectionModel.isSelected(rowValue);
        StringBuilder trClasses = new StringBuilder(evenRowStyle);
        if (isSelected) {
            trClasses.append(selectedRowStyle);
        }
        // Add custom row styles.
        RowStyles<TraceInfoProxy> rowStyles = cellTable.getRowStyles();
        if (rowStyles != null) {
            String extraRowStyles = rowStyles.getStyleNames(rowValue, absRowIndex);
            if (extraRowStyles != null) {
                trClasses.append(" ").append(extraRowStyles);
            }
        }

        buildStandardRow(rowValue, absRowIndex, isSelected, trClasses.toString());
        if (expandedDetails.contains(rowValue.getDataOffs())) {
            buildDetailRow(rowValue, absRowIndex, isSelected, trClasses.toString());
        }
    }


    private void buildDetailRow(TraceInfoProxy value, int absRowIndex, boolean isSelected, String trClasses) {
        TableRowBuilder tr = startRow();
        tr.className(trClasses);
        tr.startTD().endTD();
        TableCellBuilder td = tr.startTD().colSpan(cellTable.getColumnCount()-1);
        DivBuilder div = td.startDiv().className(extenderCellStyle);
        //div.style().outlineStyle(Style.OutlineStyle.NONE).endStyle();
        this.renderCell(div, createContext(1), colDetails, value);
        div.endDiv();
        td.endTD();
        tr.endTR();
    }


    private void buildStandardRow(TraceInfoProxy rowValue, int absRowIndex, boolean isSelected, String trClasses) {
        //boolean isEven = absRowIndex % 2 == 0;


        // Build the row.
        TableRowBuilder tr = startRow();
        tr.className(trClasses);

        // Build the columns.
        int columnCount = cellTable.getColumnCount();
        for (int curColumn = 0; curColumn < columnCount; curColumn++) {
            Column<TraceInfoProxy, ?> column = cellTable.getColumn(curColumn);
            // Create the cell styles.
            StringBuilder tdClasses = new StringBuilder(cellStyle);
            tdClasses.append(evenCellStyle);
            if (curColumn == 0) {
                tdClasses.append(firstColumnStyle);
            }
            if (isSelected) {
                tdClasses.append(selectedCellStyle);
            }
            // The first and last column could be the same column.
            if (curColumn == columnCount - 1) {
                tdClasses.append(lastColumnStyle);
            }

            // Add class names specific to the cell.
            Cell.Context context = new Cell.Context(absRowIndex, curColumn, cellTable.getValueKey(rowValue));
            String cellStyles = column.getCellStyleNames(context, rowValue);
            if (cellStyles != null) {
                tdClasses.append(" " + cellStyles);
            }

            // Build the cell.
            HasHorizontalAlignment.HorizontalAlignmentConstant hAlign = column.getHorizontalAlignment();
            HasVerticalAlignment.VerticalAlignmentConstant vAlign = column.getVerticalAlignment();
            TableCellBuilder td = tr.startTD();
            td.className(tdClasses.toString());
            if (hAlign != null) {
                td.align(hAlign.getTextAlignString());
            }
            if (vAlign != null) {
                td.vAlign(vAlign.getVerticalAlignString());
            }

            // Add the inner div.
            DivBuilder div = td.startDiv();
            div.style().outlineStyle(Style.OutlineStyle.NONE).height(14, Style.Unit.PX).endStyle();

            // Render the cell into the div.
            renderCell(div, context, column, rowValue);

            // End the cell.
            div.endDiv();
            td.endTD();
        }

        // End the row.
        tr.endTR();
    }
}
