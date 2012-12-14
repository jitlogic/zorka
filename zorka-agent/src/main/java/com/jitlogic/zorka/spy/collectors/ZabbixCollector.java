package com.jitlogic.zorka.spy.collectors;

import com.jitlogic.zorka.integ.zabbix.ZabbixTrapper;
import com.jitlogic.zorka.spy.SpyLib;
import com.jitlogic.zorka.spy.SpyProcessor;
import com.jitlogic.zorka.spy.SpyRecord;
import com.jitlogic.zorka.util.ObjectInspector;
import com.jitlogic.zorka.util.ZorkaLog;
import com.jitlogic.zorka.util.ZorkaLogger;

/**
 * Copyright 2012 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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
public class ZabbixCollector implements SpyProcessor {

    private ZorkaLog log = ZorkaLogger.getLog(this.getClass());

    private ZabbixTrapper trapper;
    private String expr, host, key;

    private ObjectInspector inspector = new ObjectInspector();

    public ZabbixCollector(ZabbixTrapper trapper, String expr, String host, String key) {
        this.trapper = trapper;
        this.expr = expr;
        this.host = host;
        this.key = key;
    }

    public SpyRecord process(int stage, SpyRecord record) {
        Object[] vals = record.getVals(SpyLib.ON_COLLECT);

        String data = inspector.substitute(expr, vals);

        if (host != null) {
            trapper.send(host, key, data);
        } else {
            trapper.send(key, data);
        }

        return record;
    }
}
