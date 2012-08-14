package com.jitlogic.zorka.mbeans;

import javax.management.j2ee.statistics.Stats;
import javax.management.openmbean.*;
import java.lang.reflect.ParameterizedType;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * This class wraps ordinary collections of objects (beans) and presents them as
 * tabular data.
 *
 * @author RLE <rafal.lewczuk@gmail.com>
 */
public class TabularDataWrapper<C,V> implements TabularData {

    private String indexName;
    private String[] attrNames;
    private String description;

    private Class<?>[] attrClasses;
    private OpenType<?>[] attrTypes;

    private CompositeType rowType;
    private TabularType tabularType;
    private C data;

    private Class<V> wrappedClass;



    public TabularDataWrapper(C data, String description, String indexName, String[] attrNames, Class<?>[] attrClasses)
            throws OpenDataException {

        this.data = data;
        this.description = description;
        this.indexName = indexName;

        this.wrappedClass = (Class<V>)((ParameterizedType)getClass()
                    .getGenericSuperclass()).getActualTypeArguments()[0];

        makeRowType(attrNames, attrClasses);

        this.tabularType = new TabularType(wrappedClass.getName(),
                description, rowType, new String[] { indexName });
    }


    private void makeRowType(String[] attrNames, Class<?>[] attrClasses) throws OpenDataException {

        this.attrNames = attrNames;
        this.attrClasses = attrClasses;
        this.attrTypes = new OpenType[attrNames.length];

        for (int i = 0; i < attrNames.length; i++) {

        }

        this.rowType = new CompositeType(wrappedClass.getName(),
                description, attrNames, attrNames, attrTypes);
    }


    public TabularType getTabularType() {
        return tabularType;
    }


    public Object[] calculateIndex(CompositeData value) {
        return new Object[] { value.get(indexName) };
    }


    public int size() {
        if (data instanceof Collection) {
            return ((Collection)data).size();
        } else if (data instanceof Map) {
            return ((Map)data).size();
        } else if (data instanceof Stats) {
            return ((Stats)data).getStatistics().length;
        }

        throw new UnsupportedOperationException("Type " + data.getClass() + " not supported.");
    }


    public boolean isEmpty() {
        return size() == 0;
    }


    public boolean containsKey(Object[] key) {
        return false;  //TODO To change body of implemented methods use File | Settings | File Templates.
    }


    public boolean containsValue(CompositeData value) {
        return false;  // TODO To change body of implemented methods use File | Settings | File Templates.
    }


    public CompositeData get(Object[] key) {
        return null;  // TODO To change body of implemented methods use File | Settings | File Templates.
    }


    public void put(CompositeData value) {
        throw new UnsupportedOperationException();
    }


    public CompositeData remove(Object[] key) {
        throw new UnsupportedOperationException();
    }


    public void putAll(CompositeData[] values) {
        throw new UnsupportedOperationException();
    }


    public void clear() {
        throw new UnsupportedOperationException();
    }


    public Set<?> keySet() {

        return null;  // TODO To change body of implemented methods use File | Settings | File Templates.
    }


    public Collection<?> values() {
        return null; // TODO
    }
}
