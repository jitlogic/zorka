/**
 * Copyright 2012-2014 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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
package com.jitlogic.zorka.core.integ;

import com.jitlogic.zorka.core.mbeans.MBeanServerRegistry;
import com.jitlogic.zorka.core.perfmon.QueryDef;
import com.jitlogic.zorka.core.perfmon.QueryLister;
import com.jitlogic.zorka.core.perfmon.QueryResult;

import java.util.*;


public class NagiosJmxCommand extends AbstractNagiosCommand {

    private QueryDef query;

    public NagiosJmxCommand(MBeanServerRegistry mBeanServerRegistry, QueryDef query) {
        super(mBeanServerRegistry);
        this.query = query.with(QueryDef.NO_NULL_ATTRS);
    }


    @Override
    public NrpePacket cmd(Object... args) {

        List<QueryResult> results = new QueryLister(mBeanServerRegistry, query).list();

        if (results.size() == 0) {
            return NrpePacket.error("No data found.");
        }

        List<Map<String,Long>> pstatList = new ArrayList<Map<String, Long>>();

        for (QueryResult result : results) {
            pstatList.add(toPstats(result));
        }

        Map<String,Long> sum = null;
        QueryResult sel = null;

        switch (selMode) {
            case SEL_FIRST:
                if (results.size() > 0) {
                    sum = pstatList.get(0);
                    sel = results.get(0);
                }
                break;
            case SEL_SUM:
                sum = sumPstats(pstatList);
                break;
            case SEL_ONE:
                for (int i = 0; i < results.size(); i++) {
                    if (selVal.equals(results.get(i).getAttr(selName))) {
                        sum = pstatList.get(i);
                        sel = results.get(i);
                    }
                }
                break;
        }

        int rcode = resultCode(sum);

        StringBuilder pktContent = new StringBuilder();

        // Tag, status and title for first result line
        pktContent.append(tag);
        pktContent.append(' ');
        pktContent.append(RC_CODES[rcode]);
        pktContent.append(" - ");
        pktContent.append(title);
        pktContent.append(" ");

        // Label and data for first line
        pktContent.append(textLine(sel, sum));

        pktContent.append("| ");
        pktContent.append(perfLine(sel, sum));

        for (int i = 0; i < results.size(); i++) {
            if (sel != results.get(i)) {
                pktContent.append('\n');
                pktContent.append(textLine(results.get(i), pstatList.get(i)));
            }
        }


        for (int i = 0; i < results.size(); i++) {
            if (sel != results.get(i)) {
                if (i > 0) {
                    pktContent.append('\n');
                } else {
                    pktContent.append("| ");
                }
                pktContent.append(perfLine(results.get(i), pstatList.get(i)));
            }
        }

        return NrpePacket.newInstance(2, NrpePacket.RESPONSE_PACKET, rcode, pktContent.toString());
    }
}
