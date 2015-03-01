/**
 * Copyright 2012-2015 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
 *
 * ZORKA is free software. You can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * ZORKA is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * ZORKA. If not, see <http://www.gnu.org/licenses/>.
 */

package com.jitlogic.zorka.core.mbeans;

import com.jitlogic.zorka.common.util.ZorkaLog;
import com.jitlogic.zorka.common.util.ZorkaLogger;
import com.jitlogic.zorka.common.util.ZorkaUtil;
import com.jitlogic.zorka.common.util.ObjectInspector;

import javax.management.openmbean.*;
import java.io.Serializable;
import java.util.*;


/**
 * This class wraps ordinary collections of objects (beans) and presents them as
 * tabular data.
 * <p/>
 * Performance note: this wrapper is intended for jconsole and similiar interactive tools.
 * Do not use it for automated monitoring, use bare objects to obtain better performance.
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class TabularDataWrapper<V> implements TabularData, Serializable {

    private final ZorkaLog log = ZorkaLogger.getLog(this.getClass());

    /**
     * This interface binds tabular data wrapper with undelying data structures.
     */
    private static interface TabularSetExtractor {

        /**
         * Returns set size (number of objects in a set).
         *
         * @return tabular set size
         */
        public int size();

        /**
         * Returns values representing table rows.
         *
         * @return collection of rows
         */
        public Collection<?> values();

        /**
         * Returns true if set contains row identified by given key.
         *
         * @param key key identifying single object
         * @return true if set contains given row
         */
        boolean containsKey(String key);

        /**
         * Returns keys identifying all records in a set.
         *
         * @return key set
         */
        Set<?> keySet();

        /**
         * Returns object from a set identified by given key.
         *
         * @param key key identifying single object
         * @return object identified by given key or null if not found
         */
        Object get(String key);
    }

    private String indexName;
    private String[] attrNames;
    private OpenType[] attrTypes;

    private CompositeType rowType;
    private TabularType tabularType;
    private Object data;

    private transient TabularSetExtractor extractor;


    /**
     * Creates tabular data wrapper.
     *
     * @param wrappedClass class of wrapped objects
     * @param data         object containing set of wrapped objects
     * @param description  table description
     * @param indexName    attribute used as key in data set
     * @param attrNames    displayed attribute names
     * @param attrTypes    types of displayed attributes (columns in resulting table)
     * @throws OpenDataException thrown from JMX Open Data code
     */
    public TabularDataWrapper(Class<?> wrappedClass, Object data, String description,
                              String indexName, String[] attrNames, OpenType[] attrTypes)
            throws OpenDataException {
        this(wrappedClass, data, description, indexName, attrNames, attrTypes, attrNames);
    }

    /**
     * Creates tabular data wrapper.
     *
     * @param wrappedClass     class of wrapped objects
     * @param data             object containing set of wrapped objects
     * @param description      table description
     * @param indexName        attribute used as key in data set
     * @param attrNames        displayed attribute names
     * @param attrTypes        types of displayed attributes (columns in resulting table)
     * @param attrDescriptions descriptions of displayed attributes
     * @throws OpenDataException thrown from JMX Open Data code
     */
    public TabularDataWrapper(Class<?> wrappedClass, Object data, String description,
                              String indexName, String[] attrNames, OpenType[] attrTypes, String[] attrDescriptions)
            throws OpenDataException {

        this.data = data;
        this.indexName = indexName;

        if (data == null) {
            throw new IllegalArgumentException("Wrapped data set cannot be null.");
        }

        if (data instanceof Collection) {
            extractor = new CollectionSetExtractor();
        } else if (data instanceof Map) {
            extractor = new MapSetExtractor();
        } else {
            throw new IllegalArgumentException("Data set of type '" + data.getClass().getName() + "' is not supported.");
        }

        this.attrNames = ZorkaUtil.copyArray(attrNames);
        this.attrTypes = ZorkaUtil.copyArray(attrTypes);

        this.rowType = new CompositeType(wrappedClass.getName(),
                description, attrNames, attrDescriptions, this.attrTypes);

        this.tabularType = new TabularType(wrappedClass.getName(),
                description, rowType, new String[]{indexName});

    }

    @Override
    public TabularType getTabularType() {
        return tabularType;
    }

    @Override
    public Object[] calculateIndex(CompositeData value) {
        return new Object[]{value.get(indexName)};
    }

    @Override
    public int size() {
        return extractor.size();
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public boolean containsKey(Object[] key) {

        if (key.length != 1 || !(key[0] instanceof String))
            throw new IllegalArgumentException();

        return extractor.containsKey((String) key[0]);
    }

    @Override
    public boolean containsValue(CompositeData value) {
        CompositeData found = get(new Object[]{value.get(indexName)});

        return found != null && found.equals(value);
    }

    @Override
    public CompositeData get(Object[] key) {

        if (key.length != 1 || !(key[0] instanceof String))
            throw new IllegalArgumentException();

        Object obj = extractor.get((String) key[0]);

        if (obj == null) {
            log.warn(ZorkaLogger.ZAG_ERRORS, "Cannot find element '" + key[0] + "'");
            return null;
        }

        Object[] fields = new Object[attrNames.length];

        for (int i = 0; i < attrNames.length; i++) {
            fields[i] = ObjectInspector.get(obj, attrNames[i]);
        }

        try {
            return new CompositeDataSupport(rowType, attrNames, fields);
        } catch (OpenDataException e) {
            log.error(ZorkaLogger.ZAG_ERRORS, "Error creating CompositeData element", e);
        }

        return null;
    }

    @Override
    public void put(CompositeData value) {
        throw new UnsupportedOperationException("This read-only view.");
    }

    @Override
    public CompositeData remove(Object[] key) {
        throw new UnsupportedOperationException("This is read-only view.");
    }

    @Override
    public void putAll(CompositeData[] values) {
        throw new UnsupportedOperationException("This is read-only view.");
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("This is read-only view.");
    }

    @Override
    public Set<?> keySet() {
        return extractor.keySet();
    }

    @Override
    public Collection<?> values() {
        return extractor.values();
    }


    /**
     * Support for collections as data sets
     */
    public class CollectionSetExtractor implements TabularSetExtractor {

        @Override
        public int size() {
            return ((Collection) data).size();
        }

        @Override
        public Collection<?> values() {
            return (Collection) data;
        }

        @Override
        public boolean containsKey(String key) {
            for (Object obj : (Collection) data) {
                if (key.equals(ObjectInspector.get(obj, indexName)))
                    return true;
            }
            return false;
        }

        @Override
        public Set<?> keySet() {
            Set<List<?>> keyset = new HashSet<List<?>>(size() * 2);

            for (Object obj : (Collection) data)
                keyset.add(Arrays.asList(ObjectInspector.get(obj, indexName)));

            return keyset;
        }

        @Override
        public Object get(String key) {
            for (Object obj : (Collection) data) {
                if (key.equals(ObjectInspector.get(obj, indexName)))
                    return obj;
            }
            return null;
        }
    }


    /**
     * Support for hash maps as data sets
     */
    private class MapSetExtractor implements TabularSetExtractor {

        @Override
        public int size() {
            return ((Map) data).size();
        }

        @Override
        public Collection<?> values() {
            return ((Map) data).values();
        }

        @Override
        public boolean containsKey(String key) {
            return ((Map) data).containsKey(key);
        }

        @Override
        public Set<?> keySet() {
            Set<List<?>> keyset = new HashSet<List<?>>(size() * 2);

            for (Object obj : ((Map) data).keySet()) {
                keyset.add(Arrays.asList(obj));
            }

            return keyset;
        }

        @Override
        public Object get(String key) {
            return ((Map) data).get(key);
        }
    }

}
