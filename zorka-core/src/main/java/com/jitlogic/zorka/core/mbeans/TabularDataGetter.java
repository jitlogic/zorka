/**
 * Copyright 2012-2015 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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
package com.jitlogic.zorka.core.mbeans;

import com.jitlogic.zorka.common.stats.ValGetter;
import com.jitlogic.zorka.common.util.ObjectInspector;
import com.jitlogic.zorka.common.util.ZorkaLog;
import com.jitlogic.zorka.common.util.ZorkaLogger;
import com.jitlogic.zorka.common.util.ZorkaUtil;

import javax.management.openmbean.*;

/**
 * Wraps iterable data and presents it as tabular data type. Instances of this class
 * are to be registered as ZorkaMappedBean attributes.
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class TabularDataGetter implements ValGetter {

    /**
     * Logger
     */
    private ZorkaLog log = ZorkaLogger.getLog(this.getClass());

    /**
     * Source field
     */
    private Object wrappedObject;

    /**
     * Object type name
     */
    private String typeName;

    /**
     * Object description
     */
    private String typeDesc;

    /**
     * Item names (field names)
     */
    private String[] itemNames;

    /**
     * Composite row type
     */
    private CompositeType rowType;

    /**
     * Tabular type of returned objects
     */
    private TabularType tableType;

    /**
     * Creates tabular data getter wrapping a type.
     *
     * @param wrappedObject wrapped object
     * @param typeName      tabular type name
     * @param typeDesc      tabular type description
     * @param indexField    index field
     * @param itemNames     field names
     * @param itemDesc      field descriptions
     * @param itemTypes     field types
     */
    public TabularDataGetter(Object wrappedObject, String typeName, String typeDesc, String indexField,
                             String[] itemNames, String[] itemDesc, OpenType[] itemTypes) {

        this.wrappedObject = wrappedObject;
        this.typeName = typeName;
        this.typeDesc = typeDesc;
        this.itemNames = ZorkaUtil.copyArray(itemNames);

        try {
            rowType = new CompositeType(typeName, typeDesc, itemNames, itemDesc, itemTypes);
            tableType = new TabularType(typeName + "Table", typeDesc + " table", rowType, new String[]{indexField});
        } catch (OpenDataException e) {
            log.error(ZorkaLogger.ZAG_ERRORS, "Error creating row type or table type", e);
        }
    }


    @Override
    public Object get() {
        TabularDataSupport table = new TabularDataSupport(tableType);

        for (Object attr : ObjectInspector.list(wrappedObject)) {
            Object obj = ObjectInspector.get(wrappedObject, attr);
            Object[] values = new Object[itemNames.length];

            for (int i = 0; i < itemNames.length; i++) {
                values[i] = ObjectInspector.get(obj, itemNames[i]);
            }

            try {
                table.put(new CompositeDataSupport(rowType, itemNames, values));
            } catch (OpenDataException e) {
                log.error(ZorkaLogger.ZAG_ERRORS, "Error creating composite data.", e);
            }
        }

        return table;
    }


    /**
     * Returns  Tabular data type
     *
     * @return tabular data type
     */
    public TabularType getTableType() {
        return tableType;
    }

    /**
     * Returns type name
     *
     * @return tabular data type name
     */
    public String getTypeName() {
        return typeName;
    }

    /**
     * Return type description
     *
     * @return type description
     */
    public String getTypeDesc() {
        return typeDesc;
    }

}
