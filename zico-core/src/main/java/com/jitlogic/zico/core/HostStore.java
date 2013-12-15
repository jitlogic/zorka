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


import com.jitlogic.zico.core.model.HostInfo;
import com.jitlogic.zico.core.model.KeyValuePair;
import com.jitlogic.zico.core.model.PagingData;
import com.jitlogic.zico.core.model.SymbolicExceptionInfo;
import com.jitlogic.zico.core.model.TraceInfo;
import com.jitlogic.zico.core.model.TraceListFilterExpression;
import com.jitlogic.zico.core.rds.RDSCleanupListener;
import com.jitlogic.zico.core.rds.RDSStore;
import com.jitlogic.zorka.common.util.ZorkaUtil;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Represents performance data store for a single agent.
 */
public class HostStore implements Closeable, RDSCleanupListener {

    private final static Logger log = LoggerFactory.getLogger(HostStore.class);

    private String rootPath;
    private RDSStore rds;
    private HostInfo hostInfo;
    private HostStoreManager manager;

    private TraceCache cache;

    private String reOp;

    private JdbcTemplate jdbc;
    private NamedParameterJdbcTemplate ndbc;


    private final RowMapper<TraceInfo> TRACE_INFO_MAPPER = new RowMapper<TraceInfo>() {
        @Override
        public TraceInfo mapRow(ResultSet rs, int rowNum) throws SQLException {
            TraceInfo info = new TraceInfo();

            info.setHostId(rs.getInt("HOST_ID"));
            info.setDataOffs(rs.getLong("DATA_OFFS"));
            info.setTraceId(rs.getInt("TRACE_ID"));
            info.setTraceType(manager.getSymbolRegistry().symbolName(info.getTraceId()));
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

            JSONObject attrs = (JSONObject)JSONValue.parse(rs.getString("ATTRS"));

            if (attrs != null) {
                List<KeyValuePair> attrList = new ArrayList<KeyValuePair>(attrs.size());

                for (Map.Entry<String,Object> e : attrs.entrySet()) {
                    attrList.add(new KeyValuePair(e.getKey(), ""+e.getValue()));
                }

                info.setAttributes(ZicoUtil.sortKeyVals(attrList));
            }

            String exJson = rs.getString("EXINFO");
            if (exJson != null && exJson.trim().length() > 0) {
                JSONObject json = (JSONObject)JSONValue.parse(exJson);
                SymbolicExceptionInfo sei = new SymbolicExceptionInfo();
                sei.setExClass(""+json.get("exClass"));
                sei.setMessage("" + json.get("message"));
                JSONArray jstack = (JSONArray)(json.get("stackTrace"));
                if (jstack != null) {
                    List<String> stack = new ArrayList<String>(jstack.size());
                    for (Object obj : jstack) {
                        stack.add(""+obj);
                    }
                    sei.setStackTrace(stack);
                }
                info.setExceptionInfo(sei);
            }

            info.setDescription(manager.getTemplater().templateDescription(info));

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

        this.reOp = "pgsql".equals(manager.getConfig().stringCfg("zico.db.type", null)) ? "~" : "regexp";
    }


    public synchronized HostStore updateInfo(ResultSet rs) throws SQLException {
        hostInfo.setId(rs.getInt("HOST_ID"));
        hostInfo.setName(rs.getString("HOST_NAME"));
        hostInfo.setAddr(rs.getString("HOST_ADDR"));
        hostInfo.setPath(rs.getString("HOST_PATH"));
        hostInfo.setPass(rs.getString("HOST_PASS"));
        hostInfo.setFlags(rs.getInt("HOST_FLAGS"));
        hostInfo.setMaxSize(rs.getLong("MAX_SIZE"));
        hostInfo.setDescription(rs.getString("HOST_DESC"));
        return this;
    }


    public synchronized HostStore updateInfo(HostInfo info) {
        hostInfo.setAddr(info.getAddr());
        hostInfo.setPass(info.getPass());
        hostInfo.setFlags(info.getFlags());
        hostInfo.setMaxSize(info.getMaxSize());
        hostInfo.setDescription(info.getDescription());
        return this;
    }


    public HostInfo getHostInfo() {
        return hostInfo;
    }


    public String toString() {
        return "zico.Store(" + hostInfo.getName() + ")";
    }


    public synchronized RDSStore getRds() {
        if (rds == null) {
            String rootPath = ZorkaUtil.path(manager.getDataDir(), hostInfo.getPath());
            String rdspath = ZorkaUtil.path(rootPath, "traces");

            File rdsDir = new File(rootPath);
            if (!rdsDir.exists()) {
                rdsDir.mkdirs();
            }

            try {
                long fileSize = getStoreManager().getConfig().kiloCfg("rds.file.size", 16 * 1024 * 1024L).intValue();
                long segmentSize = getStoreManager().getConfig().kiloCfg("rds.seg.size", 1024 * 1024L);
                rds = new RDSStore(rdspath,
                        hostInfo.getMaxSize(),
                        fileSize,
                        segmentSize, this);
                //rds.addCleanupListener(this);
            } catch (IOException e) {
                log.error("Cannot open RDS store at '" + rdspath + "'", e);
            }
        }
        return rds;
    }


    public synchronized void save() {
        jdbc.update("update HOSTS set HOST_ADDR=?, HOST_DESC=?, HOST_PASS=?, HOST_FLAGS=?, MAX_SIZE = ? where HOST_ID=?",
                hostInfo.getAddr(), hostInfo.getDescription(), hostInfo.getPass(), hostInfo.getFlags(),
                hostInfo.getMaxSize(), hostInfo.getId());
        if (rds != null) {
            rds.setMaxSize(hostInfo.getMaxSize());
            try {
                rds.cleanup();
            } catch (IOException e) {
                log.error("Error resizing RDS store for " + hostInfo.getName());
            }
        }
    }

    private final static Map<String, String> TRACES_ORDER_COLS = ZorkaUtil.map(
            "clock", "CLOCK",
            "calls", "CALLS",
            "errors", "ERRORS",
            "records", "RECORDS",
            "executionTime", "EXTIME",
            "traceType", "TRACE_ID"
    );

    private final static Set<String> TRACES_ORDER_DIRS = ZorkaUtil.set("ASC", "DESC");

    public PagingData pageTraces(int offset, int limit, TraceListFilterExpression filter) {
        String orderBy = filter.getSortBy();
        String orderDir = filter.isSortAsc() ? "ASC" : "DESC";

        if (!TRACES_ORDER_COLS.containsKey(orderBy) || !TRACES_ORDER_DIRS.contains(orderDir)) {
            throw new ZicoRuntimeException("Invalid ordering arguments: orderBy=" + orderBy + ", orderDir=" + orderDir);
        }

        MapSqlParameterSource params = new MapSqlParameterSource("hostId", hostInfo.getId());

        String sqlc = " HOST_ID = :hostId";

        if (filter.getTimeStart() != 0 && filter.getTimeEnd() != 0) {
            sqlc += " and CLOCK between :timeStart and :timeEnd";
            params.addValue("timeStart", filter.getTimeStart());
            params.addValue("timeEnd", filter.getTimeEnd());
        } else if (filter.getTimeEnd() != 0) {
            sqlc += " and CLOCK < :timeEnd";
            params.addValue("timeEnd", filter.getTimeEnd());
        } else if (filter.getTimeStart() != 0) {
            sqlc += " and CLOCK > :timeStart";
            params.addValue("timeStart", filter.getTimeStart());
        }

        if (filter.getTraceId() != 0) {
            sqlc += " and TRACE_ID = :traceId";
            params.addValue("traceId", filter.getTraceId());
        }

        if (filter.isErrorsOnly()) {
            sqlc += " and STATUS = 1";
        }

        if (filter.getFilterExpr() != null && filter.getFilterExpr().trim().length() > 0) {
            sqlc += parseFilterExpr(filter.getFilterExpr().trim(), params, reOp);
        }

        if (filter.getMinTime() > 0) {
            sqlc += " and EXTIME > :minTime";
            params.addValue("minTime", filter.getMinTime());
        }

        String sql1 = "select * from TRACES where " + sqlc + " order by " + TRACES_ORDER_COLS.get(orderBy)
                + " " + orderDir + " limit :limit offset :offset";

        params.addValue("limit", limit);
        params.addValue("offset", offset);

        List<TraceInfo> results = ndbc.query(sql1, params, TRACE_INFO_MAPPER);

        PagingData result = new PagingData();

        String sql2 = "select count(1) from TRACES where " + sqlc;

        result.setOffset(offset);
        result.setTotal(ndbc.queryForObject(sql2, params, Integer.class));
        result.setResults(results);

        return result;

    }

    private static final String QUOTED_CHARS = "*[]().\\?+{}^$";

    public static String parseFilterExpr(String expr, MapSqlParameterSource params, String reOp) {
        if (expr.startsWith("@")) {
            int ix = expr.indexOf('=');
            if (ix < 2) {
                throw new ZicoRuntimeException("Invalid filter expression: '" + expr + "'");
            }
            String sa = expr.substring(1, ix);
            String sv = expr.substring(ix + 1, expr.length());
            if (!sv.startsWith("~")) {
                StringBuilder sb = new StringBuilder(expr.length() + 10);
                for (int i = 0; i < sv.length(); i++) {
                    char ch = sv.charAt(i);
                    if (ch == '*') {
                        sb.append("[^\"]*");
                    } else if (ch == '?') {
                        sb.append("[^\"]");
                    } else {
                        if (QUOTED_CHARS.indexOf(ch) != -1) {
                            sb.append('\\');
                        }
                        sb.append(ch);
                    }
                }
                sv = sb.toString();
            } else {
                sv = sv.substring(1, sv.length()).replace(".", "[^\"]");
            }
            String regex = "\"" + sa + "\":\"" + sv + "\"";
            params.addValue("filterRegex", regex);
            return " and ATTRS " + reOp + " :filterRegex";
        }
        if (expr.startsWith("~")) {
            params.addValue("filterRegex", expr.substring(1));
            return " and ATTRS " + reOp + " :filterRegex";
        } else {
            params.addValue("filterExpr", "%" + expr.replace("*", "%") + "%");
            return " and ATTRS like :filterExpr";
        }
    }

    public TraceInfo getTrace(long traceOffs) {
        return jdbc.queryForObject("select * from TRACES where HOST_ID = ? and DATA_OFFS = ?",
                TRACE_INFO_MAPPER, hostInfo.getId(), traceOffs);
    }


    public TraceRecordStore getTraceContext(long traceOffs) {
        return new TraceRecordStore(this, getTrace(traceOffs), cache, manager.getSymbolRegistry());
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

    @Override
    public void onChunkRemoved(Long start, Long length) {
        log.info("Discarding old trace data for " + hostInfo.getName() + " start=" + start + ", length=" + length);

        if (start != null && length != null) {
            jdbc.update("delete from TRACES where HOST_ID = ? and DATA_OFFS between ? and ?",
                    hostInfo.getId(), start, start + length);
        } else if (start != null) {
            jdbc.update("delete from TRACES where HOST_ID = ? and DATA_OFFS < ?", hostInfo.getId(), start);
        }
    }


    // ------------------ entity methods ----------------------
    public Integer getId() {
        return hostInfo.getId();
    }

    public String getName() {
        return hostInfo.getName();
    }

    public String getAddr() {
        return hostInfo.getAddr();
    }

    public void setAddr(String addr) {
        hostInfo.setAddr(addr);
        save();
    }

    public String getDescription() {
        return hostInfo.getDescription();
    }

    public void setDescription(String description) {
        hostInfo.setDescription(description);
        save();
    }

    public String getPass() {
        return hostInfo.getPass();
    }

    public void setPass(String pass) {
        hostInfo.setPath(pass);
        save();
    }

    public int getFlags() {
        return hostInfo.getFlags();
    }

    public void setFlags(int flags) {
        hostInfo.setFlags(flags);
        save();
    }

    public long getMaxSize() {
        return hostInfo.getMaxSize();
    }

    public void setMaxSize(long maxSize) {
        hostInfo.setMaxSize(maxSize);
        save();
    }

    public boolean isEnabled() {
        return 0 == (hostInfo.getFlags() & HostInfo.DISABLED);
    }

    public void setEnabled(boolean enabled) {
        if (enabled) {
            hostInfo.setFlags(hostInfo.getFlags() & ~HostInfo.DISABLED);
        } else {
            hostInfo.setFlags(hostInfo.getFlags()|HostInfo.DISABLED);
        }
        save();
    }
}

