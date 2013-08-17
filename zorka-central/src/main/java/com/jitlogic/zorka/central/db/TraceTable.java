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
package com.jitlogic.zorka.central.db;


import com.jitlogic.zorka.central.CentralUtil;
import com.jitlogic.zorka.central.RDSStore;
import com.jitlogic.zorka.central.roof.RoofAction;
import com.jitlogic.zorka.central.roof.RoofEntityProxy;
import com.jitlogic.zorka.central.roof.RoofException;
import com.jitlogic.zorka.common.tracedata.FressianTraceFormat;
import com.jitlogic.zorka.common.tracedata.SymbolRegistry;
import com.jitlogic.zorka.common.tracedata.TraceRecord;
import org.fressian.FressianReader;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.ByteArrayInputStream;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Database backed trace entry set.
 */
public class TraceTable implements RoofEntityProxy {

    private int hostid;
    private DbContext db;
    private DbTableDesc tdesc;
    private JdbcTemplate jdbc;

    private RDSStore rds;
    private SymbolRegistry symbolRegistry;

    public TraceTable(RDSStore rds, DbContext dbContext, SymbolRegistry symbolRegistry, int hostid) {
        this.symbolRegistry = symbolRegistry;
        this.db = dbContext;
        this.jdbc = this.db.getJdbcTemplate();
        this.hostid = hostid;
        this.tdesc = this.db.getNamedDesc("TRACES");
        this.rds = rds;
    }


    public void save(long offs, int length, TraceRecord rec) {

        StringBuilder overview = new StringBuilder();

        if (rec.getAttrs() != null) {
            for (Map.Entry<Integer, Object> e : rec.getAttrs().entrySet()) {
                if (overview.length() > 250) {
                    break;
                }
                if (overview.length() > 0) {
                    overview.append("|");
                }
                overview.append(e.getValue());
            }
        }


        jdbc.update("insert into TRACES (HOST_ID,DATA_OFFS,TRACE_ID,DATA_LEN,CLOCK,RFLAGS,TFLAGS,CALLS,"
                + "ERRORS,RECORDS,EXTIME,OVERVIEW) " + "values(?,?,?,?,?,?,?,?,?,?,?,?)",
                hostid,
                offs,
                rec.getMarker().getTraceId(),
                length,
                rec.getClock(),
                rec.getFlags(),
                rec.getMarker().getFlags(),
                rec.getCalls(),
                rec.getErrors(),
                numRecords(rec),
                rec.getTime(),
                overview.length() > 250 ? overview.toString().substring(0, 250) : overview.toString());
    }


    private int numRecords(TraceRecord rec) {
        return 1; // TODO
    }


    @RoofAction("count")
    public int count() {
        return db.getJdbcTemplate().queryForObject("select count(1) from TRACES where HOST_ID = " + hostid, Integer.class);
    }

    private final static Pattern RE_SLASH = Pattern.compile("/");

    private TraceRecord getTraceRecord(String tid, String path) {
        DbRecord trace = (DbRecord) get(Arrays.asList(tid), Collections.EMPTY_MAP);

        if (trace != null) {
            try {
                long data_offs = trace.getL("DATA_OFFS");
                int data_len = trace.getI("DATA_LEN");
                byte[] blob = rds.read(data_offs, data_len);
                ByteArrayInputStream is = new ByteArrayInputStream(blob);
                FressianReader reader = new FressianReader(is, FressianTraceFormat.READ_LOOKUP);
                TraceRecord tr = (TraceRecord) reader.readObject();
                if (path != null && path.trim().length() > 0) {
                    ;
                    for (String p : RE_SLASH.split(path.trim())) {
                        Integer idx = Integer.parseInt(p);
                        if (idx >= 0 && idx < tr.numChildren()) {
                            tr = tr.getChild(idx);
                        } else {
                            throw new RoofException(405, "Child record of path " + path + " not found.");
                        }
                    }
                }

                return tr;
            } catch (Exception e) {
                throw new RoofException(501, "Error retrieving trace record.", e);
            }
        } else {
            throw new RoofException(405, "Trace not found.");
        }

    }

    @RoofAction("getRecord")
    public Object getRecord(String id, Map<String, String> params) {

        TraceRecord tr = getTraceRecord(id, params.get("path"));

        DbRecord rec = packRecord(tr);

        rec.put("PATH", params.containsKey("path") ? params.get("path") : "");

        return rec;
    }

    private DbRecord packRecord(TraceRecord tr) {
        DbRecord rec = new DbRecord(tdesc);
        rec.put("CALLS", tr.getCalls());
        rec.put("ERRORS", tr.getErrors());
        rec.put("XTIME", tr.getTime());
        rec.put("FLAGS", tr.getFlags());
        rec.put("METHOD", CentralUtil.prettyPrint(tr, symbolRegistry));
        rec.put("CHILDREN", tr.numChildren());
        return rec;
    }


    @RoofAction("listRecords")
    public List<DbRecord> listRecords(String id, Map<String, String> params) {
        String path = params.get("path");
        TraceRecord tr = getTraceRecord(id, path);
        List<DbRecord> recs = new ArrayList<DbRecord>();

        for (int i = 0; i < tr.numChildren(); i++) {
            DbRecord rec = packRecord(tr.getChild(i));
            rec.put("PATH", path.length() > 0 ? (path + "/" + i) : "" + i);
            recs.add(rec);
        }

        return recs;
    }

    @Override
    public List list(Map<String, String> params) {
        String sLimit = params.get("limit"), sOffset = params.get("offset");

        StringBuilder sb = new StringBuilder();

        if (sLimit != null) {
            sb.append(" limit ");
            sb.append(sLimit);
            if (sOffset != null) {
                sb.append(" offset ");
                sb.append(sOffset);
            }
        }

        return jdbc.query("select * from TRACES where HOST_ID = ? order by DATA_OFFS desc" + sb.toString(),
                new Object[]{hostid}, tdesc);
    }


    @Override
    public Object get(List<String> id, Map<String, String> params) {
        List lst = jdbc.query("select * from TRACES where HOST_ID = ? and DATA_OFFS = ?",
                new Object[]{hostid, Long.parseLong(id.get(0))}, tdesc);
        return lst.size() > 0 ? lst.get(0) : null;
    }
}
