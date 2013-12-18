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
package com.jitlogic.zico.core;


import com.google.web.bindery.requestfactory.shared.Locator;
import com.jitlogic.zico.core.ZicoConfig;
import com.jitlogic.zico.core.ZicoUtil;
import com.jitlogic.zico.core.model.KeyValuePair;
import com.jitlogic.zico.core.model.TraceInfo;
import com.jitlogic.zico.core.model.TraceTemplate;
import com.jitlogic.zorka.common.tracedata.SymbolRegistry;
import com.jitlogic.zorka.common.util.ObjectInspector;
import org.mapdb.DB;
import org.mapdb.DBMaker;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;

@Singleton
public class TraceTemplateManager extends Locator<TraceTemplate, Integer> {

    private DB db;

    private NavigableMap<Integer,TraceTemplate> templates;

    private volatile List<TraceTemplate> orderedTemplates;

    private ZicoConfig config;

    @Inject
    public TraceTemplateManager(ZicoConfig config) {
        this.config = config;
        open();
        reorder();
    }

    public void open() {
        db = DBMaker.newFileDB(new File(config.getConfDir(), "templates.db")).closeOnJvmShutdown().make();
        templates = db.getTreeMap("TEMPLATES");
    }

    public void close() {
        db.close();
        db = null;
        templates=  null;
    }

    private void reorder() {
        List<TraceTemplate> ttl = new ArrayList<>(templates.size());
        ttl.addAll(templates.values());
        Collections.sort(ttl, new Comparator<TraceTemplate>() {
            @Override
            public int compare(TraceTemplate o1, TraceTemplate o2) {
                return o1.getTraceId() != o2.getTraceId()
                        ? o2.getTraceId()-o1.getTraceId()
                        : o2.getOrder()-o1.getOrder();
            }
        });
        orderedTemplates = ttl;
    }

    public String templateDescription(SymbolRegistry symbolRegistry, TraceInfo info) {

        Map<String, Object> attrs = new HashMap<>();
        attrs.put("methodName", symbolRegistry.symbolName(info.getMethodId()));
        attrs.put("className", symbolRegistry.symbolName(info.getClassId()));
        if (info.getAttributes() != null) {
            for (KeyValuePair e : info.getAttributes()) {
                attrs.put(e.getKey(), e.getValue());
            }
        }

        for (TraceTemplate tti : orderedTemplates) {
            if (tti.getTraceId() == info.getTraceId() &&
                    (tti.getCondTemplate() == null || tti.getCondTemplate().trim().length() == 0 ||
                    ObjectInspector.substitute(tti.getCondTemplate(), attrs).matches(tti.getCondRegex()))) {
                return ObjectInspector.substitute(tti.getTemplate(), attrs);
            }
        }

        return genericTemplate(symbolRegistry, info);
    }


    public String genericTemplate(SymbolRegistry symbolRegistry, TraceInfo info) {
        StringBuilder sdesc = new StringBuilder();
        sdesc.append(symbolRegistry.symbolName(info.getTraceId()));
        if (info.getAttributes() != null) {
            for (KeyValuePair attr : info.getAttributes()) {
                sdesc.append("|");
                if (attr.getValue().length() > 50) {
                    sdesc.append(attr.getValue().substring(0, 50));
                } else {
                    sdesc.append(attr.getValue());
                }
            }
        }
        return sdesc.toString();
    }


    public List<TraceTemplate> listTemplates() {
        List<TraceTemplate> lst = new ArrayList<>(templates.size());
        lst.addAll(templates.values());
        return lst;
    }


    public synchronized int save(TraceTemplate tti) {

        if (tti.getId() == 0) {
            tti.setId(templates.size() > 0 ? templates.lastKey()+1 : 1);
        }

        templates.put(tti.getId(), tti);
        db.commit();

        return tti.getId();
    }



    public void remove(Integer tid) {
        templates.remove(tid);
        db.commit();
    }

    @Override
    public TraceTemplate create(Class<? extends TraceTemplate> aClass) {
        return new TraceTemplate();
    }

    @Override
    public TraceTemplate find(Class<? extends TraceTemplate> aClass, Integer templateId) {
        return templates.get(templateId);
    }

    @Override
    public Class<TraceTemplate> getDomainType() {
        return TraceTemplate.class;
    }

    @Override
    public Integer getId(TraceTemplate traceTemplate) {
        return traceTemplate.getId();
    }

    @Override
    public Class<Integer> getIdType() {
        return Integer.class;
    }

    @Override
    public Object getVersion(TraceTemplate traceTemplate) {
        return 1;
    }
}
