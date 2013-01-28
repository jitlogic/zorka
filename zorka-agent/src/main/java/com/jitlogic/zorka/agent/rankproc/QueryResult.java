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

package com.jitlogic.zorka.agent.rankproc;

import com.jitlogic.zorka.common.ZorkaLog;
import com.jitlogic.zorka.common.ZorkaLogger;

import java.util.LinkedHashMap;
import java.util.Map;

public class QueryResult {

    private static final ZorkaLog log = ZorkaLogger.getLog(QueryResult.class);

    private Object result;

    private Map<String,Object> objAttrs;
    private Map<String,Object> comAttrs;


    public QueryResult(Object result) {
        this.result = result;
    }


    public QueryResult(QueryResult orig, Object result) {
        this.result = result;
        if (orig.objAttrs != null) {
            objAttrs = new LinkedHashMap<String, Object>();
            objAttrs.putAll(orig.objAttrs);
        }
        if (orig.comAttrs != null) {
            comAttrs = new LinkedHashMap<String, Object>();
            comAttrs.putAll(orig.comAttrs);
        }
    }


    public void setAttr(int part, String key, Object val) {
        switch (part) {
            case QuerySegment.OBJECT_PART:
                setObjAttr(key, val);
                break;
            case QuerySegment.COMPONENT_PART:
                setComAttr(key, val);
                break;
        }
    }


    public Object getComAttr(String key) {
        return comAttrs != null ? comAttrs.get(key) : null;
    }


    public void setComAttr(String key, Object val) {
        if (comAttrs == null) {
            comAttrs = new LinkedHashMap<String, Object>();
        }
        comAttrs.put(key, val);
    }


    public Object getObjAttr(String key) {
        return objAttrs != null ? objAttrs.get(key) : null;
    }


    public void setObjAttr(String key, Object val) {
        if (objAttrs == null) {
            objAttrs = new LinkedHashMap<String, Object>();
        }
        objAttrs.put(key, val);
    }


    public void setResult(Object result) {
        this.result = result;
    }


    public Object getResult() {
        return result;
    }


}
