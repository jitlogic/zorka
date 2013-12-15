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
package com.jitlogic.zico.core.locators;


import com.google.web.bindery.requestfactory.shared.Locator;
import com.jitlogic.zico.core.model.KeyValuePair;
import com.jitlogic.zico.core.model.TraceInfo;
import com.jitlogic.zico.core.model.TraceTemplate;
import com.jitlogic.zorka.common.tracedata.SymbolRegistry;
import com.jitlogic.zorka.common.util.ObjectInspector;
import com.jitlogic.zorka.common.util.ZorkaUtil;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Singleton
public class TraceTemplateManager extends Locator<TraceTemplate, Integer> {

    private JdbcTemplate jdbc;
    private SimpleJdbcInsert jdbci;

    private SymbolRegistry symbolRegistry;
    private volatile Map<Integer, List<TraceTemplate>> templates = new HashMap<Integer, List<TraceTemplate>>();


    public RowMapper<TraceTemplate> TEMPLATE_MAPPER = new RowMapper<TraceTemplate>() {
        @Override
        public TraceTemplate mapRow(ResultSet rs, int rowNum) throws SQLException {
            TraceTemplate tti = new TraceTemplate();

            tti.setId(rs.getInt("TEMPLATE_ID"));
            tti.setTraceId(rs.getInt("TRACE_ID"));
            tti.setOrder(rs.getInt("ORDER_NUM"));
            tti.setFlags(rs.getInt("FLAGS"));
            tti.setCondTemplate(rs.getString("COND_TEMPLATE"));
            tti.setCondRegex(rs.getString("COND_PATTERN"));
            tti.setTemplate(rs.getString("TEMPLATE"));

            return tti;
        }
    };


    @Inject
    public TraceTemplateManager(DataSource ds, SymbolRegistry symbolRegistry) {
        this.symbolRegistry = symbolRegistry;

        this.jdbc = new JdbcTemplate(ds);
        this.jdbci = new SimpleJdbcInsert(ds).withTableName("TEMPLATES").usingGeneratedKeyColumns("TEMPLATE_ID")
                .usingColumns("TRACE_ID", "ORDER_NUM", "FLAGS", "COND_TEMPLATE", "COND_PATTERN", "TEMPLATE");

        reload();
    }


    public synchronized void reload() {
        Map<Integer, List<TraceTemplate>> templates = new HashMap<Integer, List<TraceTemplate>>();
        for (TraceTemplate tti : jdbc.query("select * from TEMPLATES order by ORDER_NUM", TEMPLATE_MAPPER)) {
            if (!templates.containsKey(tti.getTraceId())) {
                templates.put(tti.getTraceId(), new ArrayList<TraceTemplate>());
            }
            templates.get(tti.getTraceId()).add(tti);
        }
        this.templates = templates;
    }


    public String templateDescription(TraceInfo info) {

        if (templates.containsKey(info.getTraceId())) {
            Map<String, Object> attrs = new HashMap<String, Object>();
            attrs.put("methodName", symbolRegistry.symbolName(info.getMethodId()));
            attrs.put("className", symbolRegistry.symbolName(info.getClassId()));
            if (info.getAttributes() != null) {
                for (KeyValuePair e : info.getAttributes()) {
                    attrs.put(e.getKey(), e.getValue());
                }
            }

            for (TraceTemplate tti : templates.get(info.getTraceId())) {
                if (tti.getCondTemplate() == null || tti.getCondTemplate().trim().length() == 0 ||
                        ObjectInspector.substitute(tti.getCondTemplate(), attrs).matches(tti.getCondRegex())) {
                    return ObjectInspector.substitute(tti.getTemplate(), attrs);
                }
            }
        }

        return genericTemplate(info);
    }


    public String genericTemplate(TraceInfo info) {
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
        return jdbc.query("select * from TEMPLATES order by ORDER_NUM", TEMPLATE_MAPPER);
    }


    public int save(TraceTemplate tti) {
        if (tti.getId() != 0) {
            jdbc.update("update TEMPLATES set TRACE_ID = ?, ORDER_NUM = ?, FLAGS = ?, COND_TEMPLATE = ?," +
                    " COND_PATTERN = ?, TEMPLATE = ? where TEMPLATE_ID = ?", tti.getTraceId(), tti.getOrder(),
                    tti.getFlags(), tti.getCondTemplate(), tti.getCondRegex(), tti.getTemplate(), tti.getId());
            reload();
            return tti.getId();
        } else {
            int tid = jdbci.executeAndReturnKey(ZorkaUtil.<String, Object>map(
                    "TRACE_ID", tti.getTraceId(),
                    "ORDER_NUM", tti.getOrder(),
                    "FLAGS", tti.getFlags(),
                    "COND_TEMPLATE", tti.getCondTemplate(),
                    "COND_PATTERN", tti.getCondRegex(),
                    "TEMPLATE", tti.getTemplate())).intValue();
            reload();
            return tid;
        }
    }


    public void remove(int tid) {
        jdbc.update("delete from TEMPLATES where TEMPLATE_ID = ?", tid);
        reload();
    }

    @Override
    public TraceTemplate create(Class<? extends TraceTemplate> aClass) {
        return new TraceTemplate();
    }

    @Override
    public TraceTemplate find(Class<? extends TraceTemplate> aClass, Integer templateId) {
        List<TraceTemplate> lst = jdbc.query("select * from TEMPLATES where TEMPLATE_ID = ?", TEMPLATE_MAPPER, templateId);
        return lst.size() > 0 ? lst.get(0) : null;
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
