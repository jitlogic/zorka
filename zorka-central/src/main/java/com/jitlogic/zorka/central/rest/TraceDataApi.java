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
package com.jitlogic.zorka.central.rest;

import com.jitlogic.zorka.central.*;
import com.jitlogic.zorka.central.data.*;
import com.jitlogic.zorka.central.rds.RDSStore;
import com.jitlogic.zorka.common.tracedata.*;
import com.jitlogic.zorka.common.util.ZorkaUtil;
import org.fressian.FressianReader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import javax.sql.DataSource;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.io.ByteArrayInputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Pattern;

@Path("hosts")
public class TraceDataApi {

    private JdbcTemplate jdbc;
    private StoreManager storeManager;
    private SymbolRegistry symbolRegistry;


    private final RowMapper<HostInfo> HOST_INFO_MAPPER = new RowMapper<HostInfo>() {
        @Override
        public HostInfo mapRow(ResultSet rs, int rowNum) throws SQLException {
            HostInfo info = new HostInfo();

            info.setId(rs.getInt("HOST_ID"));
            info.setName(rs.getString("HOST_NAME"));
            info.setAddr(rs.getString("HOST_ADDR"));
            info.setPath(rs.getString("HOST_PATH"));

            return info;
        }
    };


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
            info.setCalls(rs.getLong("CALLS"));
            info.setErrors(rs.getLong("ERRORS"));
            info.setRecords(rs.getLong("RECORDS"));
            info.setExecutionTime(rs.getLong("EXTIME"));
            info.setDescription(rs.getString("DESCRIPTION"));

            return info;
        }
    };


    public TraceDataApi() {
        jdbc = new JdbcTemplate(CentralApp.getInstance().getDs());
        storeManager = CentralApp.getInstance().getStoreManager();
        symbolRegistry = CentralApp.getInstance().getSymbolRegistry();
    }


    public TraceDataApi(DataSource ds, StoreManager storeManager, SymbolRegistry symbolRegistry) {
        this.jdbc = new JdbcTemplate(ds);
        this.storeManager = storeManager;
        this.symbolRegistry = symbolRegistry;
    }


    @GET
    @Path("/list")
    @Produces(MediaType.APPLICATION_JSON)
    public List<HostInfo> getHosts() {
        return jdbc.query("select * from HOSTS order by HOST_NAME", HOST_INFO_MAPPER);
    }


    public HostInfo getHost(int hostId) {
        return jdbc.queryForObject("select * from HOSTS where HOST_ID = ?", HOST_INFO_MAPPER, hostId);
    }


    @GET
    @Path("/{hostId: [0-9]+}/count")
    @Produces(MediaType.APPLICATION_JSON)
    public int countTraces(@PathParam("hostId") int hostId) {
        return jdbc.queryForObject("select count(1) from TRACES where HOST_ID = ?", Integer.class, hostId);
    }


    @GET
    @Path("/{hostId: [0-9]+}/list")
    public List<TraceInfo> listTraces(@PathParam("hostId") int hostId,
                                      @DefaultValue("0") @QueryParam("offset") int offset,
                                      @DefaultValue("100") @QueryParam("limit") int limit) {
        return jdbc.query("select * from TRACES where HOST_ID = ? LIMIT ? OFFSET ?",
                TRACE_INFO_MAPPER, hostId, limit, offset);
    }


    @GET
    @Path("/{hostId: [0-9]+}/{traceId: [0-9]+}")
    @Produces(MediaType.APPLICATION_JSON)
    public TraceInfo getTrace(@PathParam("hostId") int hostId, @PathParam("traceId") long traceOffs) {
        return jdbc.queryForObject("select * from TRACES where HOST_ID = ? and DATA_OFFS = ?",
                TRACE_INFO_MAPPER, hostId, traceOffs);
    }

    private final static Map<String, String> TRACES_ORDER_COLS = ZorkaUtil.map(
            "clock", "CLOCK",
            "calls", "CALLS",
            "errors", "ERRORS",
            "records", "RECORDS",
            "description", "OVERVIEW",
            "executionTime", "EXTIME"
    );

    private final static Set<String> TRACES_ORDER_DIRS = ZorkaUtil.set("ASC", "DESC");

    @GET
    @Path("/{hostId: [0-9]+}/page")
    public PagingData<TraceInfo> pageTraces(@PathParam("hostId") int hostId,
                                            @DefaultValue("0") @QueryParam("offset") int offset,
                                            @DefaultValue("100") @QueryParam("limit") int limit,
                                            @DefaultValue("CLOCK") @QueryParam("orderBy") String orderBy,
                                            @DefaultValue("DESC") @QueryParam("orderDir") String orderDir) {

        if (!TRACES_ORDER_COLS.containsKey(orderBy) || !TRACES_ORDER_DIRS.contains(orderDir)) {
            throw new RuntimeException("Invalid ordering arguments: orderBy=" + orderBy + ", orderDir=" + orderDir);
        }

        List<TraceInfo> results = jdbc.query("select * from TRACES where HOST_ID = ? order by "
                + TRACES_ORDER_COLS.get(orderBy) + " " + orderDir + " limit ? offset ?",
                TRACE_INFO_MAPPER, hostId, limit, offset);

        PagingData result = new PagingData();

        result.setOffset(offset);
        result.setTotal(jdbc.queryForObject("select count(1) from TRACES where HOST_ID = ?", Integer.class, hostId));
        result.setResults(results);

        return result;
    }


    public HostInfo getOrCreateHost(String hostName, String hostAddr) {
        List<HostInfo> lst = jdbc.query("select * from HOSTS where HOST_NAME = ?",
                HOST_INFO_MAPPER, hostName);
        if (lst.size() == 0) {
            jdbc.update("insert into HOSTS (HOST_NAME,HOST_ADDR,HOST_PATH) values (?,?,?)",
                    hostName, hostAddr, safePath(hostName));
            return getOrCreateHost(hostName, hostAddr);
        } else {
            return lst.get(0);
        }
    }


    private String safePath(String hostname) {
        return hostname;
    }


    private final static Pattern RE_SLASH = Pattern.compile("/");

    private TraceRecord getTraceRecord(int hostId, long traceOffs, String path) {
        HostInfo host = getHost(hostId);
        TraceInfo trace = getTrace(hostId, traceOffs);

        RDSStore rds = storeManager.get(host.getName()).getRds();

        if (trace != null) {
            try {
                byte[] blob = rds.read(trace.getDataOffs(), trace.getDataLen());
                ByteArrayInputStream is = new ByteArrayInputStream(blob);
                FressianReader reader = new FressianReader(is, FressianTraceFormat.READ_LOOKUP);
                TraceRecord tr = (TraceRecord) reader.readObject();
                if (path != null && path.trim().length() > 0) {
                    for (String p : RE_SLASH.split(path.trim())) {
                        Integer idx = Integer.parseInt(p);
                        if (idx >= 0 && idx < tr.numChildren()) {
                            tr = tr.getChild(idx);
                        } else {
                            throw new RuntimeException("Child record of path " + path + " not found.");
                        }
                    }
                }

                return tr;
            } catch (Exception e) {
                throw new RuntimeException("Error retrieving trace record.", e);
            }
        } else {
            throw new RuntimeException("Trace not found.");
        }
    }

    private TraceRecordInfo packTraceRecord(TraceRecord tr, String path) {
        TraceRecordInfo info = new TraceRecordInfo();

        info.setCalls(tr.getCalls());
        info.setErrors(tr.getErrors());
        info.setTime(tr.getTime());
        info.setFlags(tr.getFlags());
        info.setMethod(CentralUtil.prettyPrint(tr, symbolRegistry));
        info.setChildren(tr.numChildren());
        info.setPath(path);

        if (tr.getAttrs() != null) {
            Map<String, String> nattr = new HashMap<String, String>();
            for (Map.Entry<Integer, Object> e : tr.getAttrs().entrySet()) {
                String s = "" + e.getValue();
                if (s.length() > 250) {
                    s = s.substring(0, 250) + "...";
                }
                nattr.put(symbolRegistry.symbolName(e.getKey()), s);
            }
            info.setAttributes(nattr);
        }

        SymbolicException sex = tr.findException();
        if (sex != null) {
            SymbolicExceptionInfo sei = new SymbolicExceptionInfo();
            sei.setExClass(symbolRegistry.symbolName(sex.getClassId()));
            sei.setMessage(sex.getMessage());
            List<String> stack = new ArrayList<String>(sex.getStackTrace().length);
            for (SymbolicStackElement sel : sex.getStackTrace()) {
                stack.add("  at " + symbolRegistry.symbolName(sel.getClassId())
                        + "." + symbolRegistry.symbolName(sel.getMethodId())
                        + " [" + symbolRegistry.symbolName(sel.getFileId())
                        + ":" + sel.getLineNum() + "]");
            }
            sei.setStackTrace(stack);
            info.setExceptionInfo(sei);
        }

        return info;
    }


    @GET
    @Path("/{hostId: [0-9]+}/{traceOffs: [0-9]+}/get")
    @Produces(MediaType.APPLICATION_JSON)
    public TraceRecordInfo getRecord(
            @PathParam("hostId") int hostId,
            @PathParam("traceOffs") long traceOffs,
            @DefaultValue("") @QueryParam("path") String path) {

        TraceRecord tr = getTraceRecord(hostId, traceOffs, path);
        return packTraceRecord(tr, path);
    }


    @GET
    @Path("/{hostId: [0-9]+}/{traceOffs: [0-9]+}/list")
    @Produces(MediaType.APPLICATION_JSON)
    public List<TraceRecordInfo> listRecords(
            @PathParam("hostId") int hostId,
            @PathParam("traceOffs") long traceOffs,
            @DefaultValue("") @QueryParam("path") String path) {
        TraceRecord tr = getTraceRecord(hostId, traceOffs, path);
        List<TraceRecordInfo> lst = new ArrayList<TraceRecordInfo>();

        if (tr != null) {
            for (int i = 0; i < tr.numChildren(); i++) {
                lst.add(packTraceRecord(tr.getChild(i), path.length() > 0 ? (path + "/" + i) : "" + i));
            }
        }

        return lst;
    }


}
