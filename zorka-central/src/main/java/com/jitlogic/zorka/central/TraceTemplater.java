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
package com.jitlogic.zorka.central;


import com.jitlogic.zorka.central.data.TraceInfo;
import com.jitlogic.zorka.central.data.TraceTemplateInfo;
import com.jitlogic.zorka.common.tracedata.SymbolRegistry;
import com.jitlogic.zorka.common.util.ObjectInspector;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TraceTemplater {

    private JdbcTemplate jdbc;

    private SymbolRegistry symbolRegistry;
    private volatile Map<Integer, List<TraceTemplateInfo>> templates = new HashMap<Integer, List<TraceTemplateInfo>>();


    private RowMapper<TraceTemplateInfo> TEMPLATE_MAPPER = new RowMapper<TraceTemplateInfo>() {
        @Override
        public TraceTemplateInfo mapRow(ResultSet rs, int rowNum) throws SQLException {
            TraceTemplateInfo tti = new TraceTemplateInfo();

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


    public TraceTemplater(DataSource ds, SymbolRegistry symbolRegistry) {
        this.symbolRegistry = symbolRegistry;

        this.jdbc = new JdbcTemplate(ds);

        reload();
    }


    public synchronized void reload() {
        Map<Integer, List<TraceTemplateInfo>> templates = new HashMap<Integer, List<TraceTemplateInfo>>();
        for (TraceTemplateInfo tti : jdbc.query("select * from TEMPLATES order by ORDER_NUM", TEMPLATE_MAPPER)) {
            if (!templates.containsKey(tti.getTraceId())) {
                templates.put(tti.getTraceId(), new ArrayList<TraceTemplateInfo>());
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
                for (Map.Entry<String, String> e : info.getAttributes().entrySet()) {
                    attrs.put(e.getKey(), e.getValue());
                }
            }

            for (TraceTemplateInfo tti : templates.get(info.getTraceId())) {
                if (tti.getCondTemplate() == null ||
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
            for (Map.Entry<String, String> e : info.getAttributes().entrySet()) {
                sdesc.append("|");
                if (e.getValue().length() > 50) {
                    sdesc.append(e.getValue().substring(0, 50));
                } else {
                    sdesc.append(e.getValue());
                }
            }
        }
        return sdesc.toString();
    }
}
