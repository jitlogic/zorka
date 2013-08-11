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


import com.jitlogic.zorka.common.tracedata.SymbolRegistry;
import com.jitlogic.zorka.common.tracedata.TraceRecord;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;

/**
 * Database backed trace entry set.
 */
public class TraceTable implements Closeable {

    private int hostid;
    private DbContext db;
    private JdbcTemplate template;
    private SymbolRegistry symbolRegistry;

    public TraceTable(DbContext dbContext, SymbolRegistry symbolRegistry, int hostid) {
        this.symbolRegistry = symbolRegistry;
        this.db = dbContext;
        this.template = this.db.getJdbcTemplate();
        this.hostid = hostid;


    }


    @Override
    public void close() throws IOException {
    }


    public void save(long offs, int length, TraceRecord rec) {

        StringBuilder overview = new StringBuilder();

        if (rec.getAttrs() != null) {
            for (Map.Entry<Integer,Object> e : rec.getAttrs().entrySet()) {
                if (overview.length() > 250) { break; }
                if (overview.length() > 0) { overview.append("|"); }
                overview.append(e.getValue());
            }
        }



        template.update("insert into TRACES (HOST_ID,DATA_OFFS,TRACE_ID,DATA_LEN,CLOCK,RFLAGS,TFLAGS,CALLS,"
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
        return 1;
    }


    public int size() {
        return db.getJdbcTemplate().queryForObject("select count(1) from TRACES", Integer.class);
    }

}
