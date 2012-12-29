/**
 * Copyright 2012 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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
package com.jitlogic.zorka.mbeans;

import com.jitlogic.zorka.util.ObjectInspector;
import com.jitlogic.zorka.logproc.ZorkaLog;
import com.jitlogic.zorka.logproc.ZorkaLogger;

import javax.management.openmbean.*;

public class TabularDataGetter implements ValGetter {

    private ZorkaLog log = ZorkaLogger.getLog(this.getClass());

    private Object source;
    private String typeName, typeDesc;
    private String[] itemNames;
    private CompositeType rowType;
    private TabularType tableType;

    private ObjectInspector inspector = new ObjectInspector();

    public TabularDataGetter(Object source, String typeName, String typeDesc, String indexField,
                             String[] itemNames, String[] itemDesc, OpenType[] itemTypes) {

        this.source = source;
        this.typeName = typeName;
        this.typeDesc = typeDesc;
        this.itemNames = itemNames;

        try {
            rowType = new CompositeType(typeName, typeDesc, itemNames, itemDesc, itemTypes);
            tableType = new TabularType(typeName + "Table", typeDesc + " table", rowType, new String[] { indexField });
        } catch (OpenDataException e) {
            log.error("Error creating row type or table type", e);
        }
    }


    public Object get() {
        TabularDataSupport table = new TabularDataSupport(tableType);

        for (Object attr : inspector.list(source)) {
            Object obj = inspector.get(source, attr);
            Object[] values = new Object[itemNames.length];

            for (int i = 0; i < itemNames.length; i++) {
                values[i] = inspector.get(obj, itemNames[i]);
            }

            try {
                table.put(new CompositeDataSupport(rowType, itemNames, values));
            } catch (OpenDataException e) {
                log.error("Error creating composite data.", e);
            }
        }

        return table;
    }

    public TabularType getTableType() {
        return tableType;
    }

    public String getTypeName() {
        return typeName;
    }

    public String getTypeDesc() {
        return typeDesc;
    }

}
