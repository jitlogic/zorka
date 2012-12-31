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
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this software. If not, see <http://www.gnu.org/licenses/>.
 */

package com.jitlogic.zorka.spy.processors;

import com.jitlogic.zorka.spy.SpyContext;
import com.jitlogic.zorka.spy.probes.SpyProbe;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class represents Spy data records.
 */
public class SpyRecord {

    private SpyContext ctx;

    private Map<String,Object> data = new HashMap<String, Object>();

    private int stages = 0, stage = -1;


    public SpyRecord(SpyContext ctx) {
        this.ctx = ctx;
    }

    public SpyRecord(SpyRecord orig, String...attrsToCopy) {
        this.ctx = orig.ctx;
        this.stage = orig.stage;
        for (String attr : attrsToCopy) {
            data.put(attr, orig.data.get(attr));
        }
    }

    public SpyRecord feed(int stage, Object[] vals) {
        List<SpyProbe> probes = ctx.getSpyDefinition().getProbes(stage);

        // TODO check if vals.length == probes.size() and log something here ...

        for (int i = 0; i < probes.size(); i++) {
            SpyProbe probe = probes.get(i);
            data.put(probe.getKey(), probe.processVal(vals[i]));
        }

        stages |= (1 << stage);
        this.stage = stage;

        return this;
    }


    public boolean hasStage(int stage) {
        return 0 != (stages & (1 << stage));
    }


    public SpyContext getContext() {
        return ctx;
    }


    public int size() {
        return data.size();
    }


    public Object get(String key) {
        return data.get(key);
    }


    public void put(String key, Object val) {
        data.put(key, val);
    }

    public void setStage(int stage) {
        this.stage = stage;
    }

    public int getStage() {
        return stage;
    }
}
