/**
 * Copyright 2012-2015 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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

package com.jitlogic.zorka.core.integ.zabbix;

import com.jitlogic.zorka.common.util.ZorkaLog;
import com.jitlogic.zorka.common.util.ZorkaLogger;
import com.jitlogic.zorka.common.util.ZorkaRuntimeException;
import com.jitlogic.zorka.core.integ.QueryTranslator;

import java.util.HashSet;
import java.util.Set;

public class ZabbixQueryTranslator implements QueryTranslator {

    private static final ZorkaLog log = ZorkaLogger.getLog(ZabbixQueryTranslator.class);

    private Set<String> allowed = new HashSet<String>();

    public void allow(String fn) {
        allowed.add(fn);
    }

    /**
     * Translates zabbix query to beanshell call
     *
     * @param query zabbix query
     * @return query ready to be passed to bsh agent
     */
    @Override
    public String translate(String query) {
        StringBuilder sb = new StringBuilder(query.length());
        int pos = 0;

        while (pos < query.length() && query.charAt(pos) != '[') {
            pos++;
        }

        String fn = query.substring(0, pos).replace("__", ".").trim();

        isAllowed(fn);

        sb.append(fn);

        if (pos >= query.length()) {
            return sb.toString();
        }

        sb.append('(');
        pos++;

        while (pos < query.length() && query.charAt(pos) != ']') {
            if (query.charAt(pos) == '"') {
                int pstart = pos++;
                while (pos < query.length() && query.charAt(pos) != '"') {
                    pos++;
                }
                sb.append(query.substring(pstart, pos + 1));
            } else {
                sb.append(query.charAt(pos));
            }
            pos++;
        }

        sb.append(')');

        return sb.toString();
    }

    private void isAllowed(String fn) {
        if (allowed.size() > 0 && !allowed.contains(fn)) {
            throw new ZorkaRuntimeException("Calling '" + fn + "' is not allowed.");
        }
    }

}
