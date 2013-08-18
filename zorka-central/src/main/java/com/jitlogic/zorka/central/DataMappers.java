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
import com.jitlogic.zorka.central.data.TraceInfo;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class DataMappers {

    public final static RowMapper<HostInfo> HOST_INFO_MAPPER = new RowMapper<HostInfo>() {
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


    public final static RowMapper<TraceInfo> TRACE_INFO_MAPPER = new RowMapper<TraceInfo>() {
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
            info.setDescription(rs.getString("OVERVIEW"));

            return info;
        }
    };


}
