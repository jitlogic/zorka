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


import com.jitlogic.zorka.central.data.HostInfo;
import com.jitlogic.zorka.central.data.PagingData;
import com.jitlogic.zorka.central.data.TraceInfo;
import com.jitlogic.zorka.central.data.TraceListFilterExpression;
import com.jitlogic.zorka.central.rds.RDSStore;
import com.jitlogic.zorka.common.tracedata.SymbolRegistry;
import com.jitlogic.zorka.common.util.ZorkaUtil;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Represents performance data store for a single agent.
 */
public class HostStore implements Closeable {

    private final static Logger log = LoggerFactory.getLogger(HostStore.class);

    private ObjectMapper mapper = new ObjectMapper();


    private String rootPath;
    private RDSStore rds;
    private HostInfo hostInfo;
    private HostStoreManager manager;
    private TraceCache cache;


    private JdbcTemplate jdbc;
    private NamedParameterJdbcTemplate ndbc;


    private final RowMapper<TraceInfo> TRACE_INFO_MAPPER = new RowMapper<TraceInfo>() {
        @Override
        public TraceInfo mapRow(ResultSet rs, int rowNum) throws SQLException {
            TraceInfo info = new TraceInfo();

            info.setHostId(rs.getInt("HOST_ID"));
            info.setDataOffs(rs.getLong("DATA_OFFS"));
            info.setTraceId(rs.getInt("TRACE_ID"));
            info.setDataLen(rs.getInt("DATA_LEN"));
            info.setClock(rs.getLong("CLOCK"));
            info.setMethodFlags(rs.getInt("RFLAGS"));
            info.setTraceFlags(rs.getInt("TFLAGS"));
            info.setStatus(rs.getInt("STATUS"));
            info.setClassId(rs.getInt("CLASS_ID"));
            info.setMethodId(rs.getInt("METHOD_ID"));
            info.setSignatureId(rs.getInt("SIGN_ID"));
            info.setCalls(rs.getLong("CALLS"));
            info.setErrors(rs.getLong("ERRORS"));
            info.setRecords(rs.getLong("RECORDS"));
            info.setExecutionTime(rs.getLong("EXTIME"));

            Map<String, String> attrs = new HashMap<String, String>();

            try {
                attrs = mapper.readValue(rs.getString("ATTRS"), Map.class);
            } catch (IOException e) {
                log.error("Error unpacking JSON attrs", e);
            }

            info.setAttributes(attrs);

            info.setDescription(manager.getTraceTemplater().templateDescription(info));

            return info;
        }
    };


    public HostStore(HostStoreManager manager,
                     TraceCache cache,
                     ResultSet rs) throws SQLException {

        this.cache = cache;
        this.manager = manager;

        this.hostInfo = new HostInfo();

        this.jdbc = new JdbcTemplate(manager.getDs());
        this.ndbc = new NamedParameterJdbcTemplate(manager.getDs());

        updateInfo(rs);

        this.rootPath = ZorkaUtil.path(manager.getDataDir(), hostInfo.getPath());
    }


    public synchronized void updateInfo(ResultSet rs) throws SQLException {
        hostInfo.setId(rs.getInt("HOST_ID"));
        hostInfo.setName(rs.getString("HOST_NAME"));
        hostInfo.setAddr(rs.getString("HOST_ADDR"));
        hostInfo.setPath(rs.getString("HOST_PATH"));
        hostInfo.setPass(rs.getString("HOST_PASS"));
        hostInfo.setFlags(rs.getInt("HOST_FLAGS"));
        hostInfo.setDescription(rs.getString("HOST_DESC"));
    }


    public synchronized void updateInfo(HostInfo info) {
        hostInfo.setAddr(info.getAddr());
        hostInfo.setPass(info.getPass());
        hostInfo.setFlags(info.getFlags());
        hostInfo.setDescription(info.getDescription());
    }


    public HostInfo getHostInfo() {
        return hostInfo;
    }


    public String toString() {
        return "central.Store(" + hostInfo.getName() + ")";
    }


    public RDSStore getRds() {
        if (rds == null) {
            String rootPath = ZorkaUtil.path(manager.getDataDir(), hostInfo.getPath());
            String rdspath = ZorkaUtil.path(rootPath, "traces");

            File rdsDir = new File(rootPath);
            if (!rdsDir.exists()) {
                rdsDir.mkdirs();
            }

            try {
                rds = new RDSStore(rdspath,
                        getStoreManager().getConfig().kiloCfg("rds.file.size", 16 * 1024 * 1024L).intValue(),
                        getStoreManager().getConfig().kiloCfg("rds.max.size", 256 * 1024 * 1024L),
                        getStoreManager().getConfig().kiloCfg("rds.seg.size", 1024 * 1024L));
            } catch (IOException e) {
                log.error("Cannot open RDS store at '" + rdspath + "'", e);
            }
        }
        return rds;
    }


    public void save() {
        jdbc.update("update HOSTS set HOST_ADDR=?, HOST_DESC=?, HOST_PASS=?, HOST_FLAGS=? where HOST_ID=?",
                hostInfo.getAddr(), hostInfo.getDescription(), hostInfo.getPass(), hostInfo.getFlags(), hostInfo.getId());
    }

    private final static Map<String, String> TRACES_ORDER_COLS = ZorkaUtil.map(
            "clock", "CLOCK",
            "calls", "CALLS",
            "errors", "ERRORS",
            "records", "RECORDS",
            "executionTime", "EXTIME"
    );

    private final static Set<String> TRACES_ORDER_DIRS = ZorkaUtil.set("ASC", "DESC");

    public PagingData<TraceInfo> pageTraces(int offset, int limit, TraceListFilterExpression filter) {
        String orderBy = filter.getSortBy();
        String orderDir = filter.isSortAsc() ? "ASC" : "DESC";

        if (!TRACES_ORDER_COLS.containsKey(orderBy) || !TRACES_ORDER_DIRS.contains(orderDir)) {
            throw new RuntimeException("Invalid ordering arguments: orderBy=" + orderBy + ", orderDir=" + orderDir);
        }

        String sql1 = "select * from TRACES where HOST_ID = :hostId";
        String sql2 = "select count(1) from TRACES where HOST_ID = :hostId";
        MapSqlParameterSource params = new MapSqlParameterSource("hostId", hostInfo.getId());

        if (filter.isErrorsOnly()) {
            sql1 += " and STATUS = 1";
            sql2 += " and STATUS = 1";
        }

        if (filter.getFilterExpr() != null && filter.getFilterExpr().trim().length() > 0) {
            sql1 += " and ATTRS like :filterExpr";
            sql2 += " and ATTRS like :filterExpr";
            params.addValue("filterExpr", "%" + filter.getFilterExpr().trim().replace("*", "%") + "%");
        }

        if (filter.getMinTime() > 0) {
            sql1 += " and EXTIME > :minTime";
            sql2 += " and EXTIME > :minTime";
            params.addValue("minTime", filter.getMinTime());
        }

        sql1 += " order by " + TRACES_ORDER_COLS.get(orderBy) + " " + orderDir + " limit :limit offset :offset";

        params.addValue("limit", limit);
        params.addValue("offset", offset);

        List<TraceInfo> results = ndbc.query(sql1, params, TRACE_INFO_MAPPER);

        PagingData result = new PagingData();


        result.setOffset(offset);
        result.setTotal(ndbc.queryForObject(sql2, params, Integer.class));
        result.setResults(results);

        return result;

    }

    public TraceInfo getTrace(long traceOffs) {
        return jdbc.queryForObject("select * from TRACES where HOST_ID = ? and DATA_OFFS = ?",
                TRACE_INFO_MAPPER, hostInfo.getId(), traceOffs);
    }


    public TraceContext getTraceContext(long traceOffs) {
        return new TraceContext(this, getTrace(traceOffs), cache, manager.getSymbolRegistry());
    }


    @Override
    public synchronized void close() throws IOException {

        try {
            if (rds != null) {
                rds.close();
                rds = null;
            }
        } catch (IOException e) {
            log.error("Cannot close RDS store '" + rds + "' for " + hostInfo.getName(), e);
        }
    }


    public String getRootPath() {
        return rootPath;
    }


    public HostStoreManager getStoreManager() {
        return manager;
    }
}
