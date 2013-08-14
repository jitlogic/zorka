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

import com.google.gwt.cell.client.TextCell;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.DataGrid;

import java.util.Date;

public class RoofDataColumnRenderers {

    public static Column<RoofRecord,String> textColumn(DataGrid<RoofRecord> table,
        final String fieldName, String columnName, String width) {

        Column<RoofRecord,String> column = new Column<RoofRecord,String>(new TextCell()) {
            @Override
            public String getValue(RoofRecord object) {
                return object.getS(fieldName);
            }
        };

        if (null != columnName) {
            table.addColumn(column, columnName);
        }

        if (null != width) {
            table.setColumnWidth(column, width);
        }

        return column;
    }


    public static Column<RoofRecord,String> tstampColumn(DataGrid<RoofRecord> table,
        final String fieldName, String columnName, String width) {

        Column<RoofRecord,String> column = new Column<RoofRecord,String>(new TextCell()) {
            @Override
            public String getValue(RoofRecord object) {
                long t = Long.parseLong(object.getS(fieldName));
                Date d = new Date(t);
                return DateTimeFormat.getFormat("yyyy-MM-dd HH:mm:ss").format(d)
                    + "." + NumberFormat.getFormat("000").format(t%1000);
            }
        };

        if (null != columnName) {
            table.addColumn(column, columnName);
        }

        if (null != width) {
            table.setColumnWidth(column, width);
        }

        return column;
    }

    public static Column<RoofRecord,String> durationColumn(DataGrid<RoofRecord> table,
        final String fieldName, String columnName, String width) {

        Column<RoofRecord,String> column = new Column<RoofRecord,String>(new TextCell()) {
            @Override
            public String getValue(RoofRecord object) {
                long ns = Long.parseLong(object.getS(fieldName));
                double t = 1.0 * ns / 1000000.0;
                String u = "ms";

                if (t > 1000.0) {
                    t /= 1000.0;
                    u = "s";
                }

                return t > 10
                    ? NumberFormat.getFormat("#####").format(t) + u
                    : NumberFormat.getFormat("###.00").format(t) + u;
            }
        };

        if (null != columnName) {
            table.addColumn(column, columnName);
        }

        if (null != width) {
            table.setColumnWidth(column, width);
        }

        return column;
    }
}
