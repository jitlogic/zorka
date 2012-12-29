/**
 * Copyright 2012 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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

package com.jitlogic.zorka.mbeans;

import com.jitlogic.zorka.util.ObjectInspector;
import com.jitlogic.zorka.logproc.ZorkaLog;
import com.jitlogic.zorka.logproc.ZorkaLogger;
import com.jitlogic.zorka.util.ZorkaUtil;

import javax.management.openmbean.*;
import java.io.Serializable;
import java.util.*;


/**
 * This class wraps ordinary collections of objects (beans) and presents them as
 * tabular data.
 *
 * Performance note: this wrapper is intended for jconsole and similiar interactive tools.
 * Do not use it for automated monitoring, use bare objects to obtain better performance.
 *
 * @author RLE <rafal.lewczuk@gmail.com>
 */
public class TabularDataWrapper<V> implements TabularData, Serializable {

    private final ZorkaLog log = ZorkaLogger.getLog(this.getClass());

    private static interface TabularSetExtractor {
        public int size();
        public Collection<?> values();
        boolean containsKey(String key);
        Set<?> keySet();
        Object get(String key);
    }

    private String indexName;
    private String[] attrNames;
    private OpenType[] attrTypes;

    private CompositeType rowType;
    private TabularType tabularType;
    private Object data;

    private Class<?> wrappedClass;

    private TabularSetExtractor extractor;


    public TabularDataWrapper(Class<?> wrappedClass, Object data, String description,
                              String indexName, String[] attrNames, OpenType[] attrTypes)
        throws OpenDataException {
        this(wrappedClass, data, description, indexName, attrNames, attrTypes, attrNames);
    }

    public TabularDataWrapper(Class<?> wrappedClass, Object data, String description,
            String indexName, String[] attrNames, OpenType[] attrTypes, String[] attrDescriptions)
            throws OpenDataException {

        this.data = data;
        this.indexName = indexName;

        this.wrappedClass = wrappedClass;

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
                description, rowType, new String[] { indexName });

    }


    public TabularType getTabularType() {
        return tabularType;
    }


    public Object[] calculateIndex(CompositeData value) {
        return new Object[] { value.get(indexName) };
    }


    public int size() {
        return extractor.size();
    }


    public boolean isEmpty() {
        return size() == 0;
    }


    public boolean containsKey(Object[] key) {

        if (key.length != 1 || !(key[0] instanceof String))
            throw new IllegalArgumentException();

        return extractor.containsKey((String)key[0]);
    }


    public boolean containsValue(CompositeData value) {
        CompositeData found = get(new Object[]{value.get(indexName)});

        return found != null && found.equals(value);
    }


    public CompositeData get(Object[] key) {

        if (key.length != 1 || !(key[0] instanceof String))
            throw new IllegalArgumentException();

        Object obj = extractor.get((String)key[0]);

        if (obj == null) {
            log.warn("Cannot find element '" + key[0] + "'");
            return null;
        }

        Object[] fields = new Object[attrNames.length];

        for (int i = 0; i < attrNames.length; i++) {
            fields[i] = ObjectInspector.get(obj, attrNames[i]);
        }

        try {
            return new CompositeDataSupport(rowType, attrNames, fields);
        } catch (OpenDataException e) {
            log.error("Error creating CompositeData element", e);
        }

        return null;
    }


    public void put(CompositeData value) {
        throw new UnsupportedOperationException("This read-only view.");
    }


    public CompositeData remove(Object[] key) {
        throw new UnsupportedOperationException("This is read-only view.");
    }


    public void putAll(CompositeData[] values) {
        throw new UnsupportedOperationException("This is read-only view.");
    }


    public void clear() {
        throw new UnsupportedOperationException("This is read-only view.");
    }


    public Set<?> keySet() {
        return extractor.keySet();
    }


    public Collection<?> values() {
        return extractor.values();
    }


    /**
     * Support for collections as data sets
     */
    public class CollectionSetExtractor implements TabularSetExtractor {

        public int size() {
            return ((Collection)data).size();
        }

        public Collection<?> values() {
            return (Collection)data;
        }

        public boolean containsKey(String key) {
            for (Object obj : (Collection)data) {
                if (key.equals(ObjectInspector.get(obj, indexName)))
                    return true;
            }
            return false;
        }

        public Set<?> keySet() {
            Set<List<?>> keyset = new HashSet<List<?>>(size()*2);

            for (Object obj : (Collection)data)
                keyset.add(Arrays.asList(ObjectInspector.get(obj, indexName)));

            return keyset;
        }

        public Object get(String key) {
            for (Object obj : (Collection)data) {
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

        public int size() {
            return ((Map)data).size();
        }

        public Collection<?> values() {
            return ((Map)data).values();
        }

        public boolean containsKey(String key) {
            return ((Map)data).containsKey(key);
        }

        public Set<?> keySet() {
            Set<List<?>> keyset = new HashSet<List<?>>(size()*2);

            for (Object obj : ((Map)data).keySet()) {
                keyset.add(Arrays.asList(obj));
            }

            return keyset;
        }

        public Object get(String key) {
            return ((Map)data).get(key);
        }
    }

    // TODO toString()

    // TODO hashCode()
}
