/**
 * Copyright 2012 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
 *
 * ZORKA is free software. You can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * ZORKA is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * ZORKA. If not, see <http://www.gnu.org/licenses/>.
 */

package com.jitlogic.zorka.spy;

import com.jitlogic.zorka.util.ObjectInspector;
import com.jitlogic.zorka.util.ZorkaUtil;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This is API for zorka users.
 */
public class SpyLib {

    public final static int GT = 0;
    public final static int GE = 1;
    public final static int EQ = 2;
    public final static int LE = 3;
    public final static int LT = 4;
    public final static int NE = 5;


    public static final int ON_ENTER   = 0;
    public static final int ON_RETURN  = 1;
    public static final int ON_ERROR   = 2;
    public static final int ON_SUBMIT  = 3;
    public static final int ON_COLLECT = 4;

    public static final int FETCH_TIME   = -1;
    public static final int FETCH_RETVAL = -2;
    public static final int FETCH_ERROR  = -3;
    public static final int FETCH_THREAD = -4;
    public static final int FETCH_CLASS  = -5;
    public static final int FETCH_NULL   = -6;

    public static int AC_PUBLIC       = 0x0001;
    public static int AC_PRIVATE      = 0x0002;
    public static int AC_PROTECTED    = 0x0004;
    public static int AC_STATIC       = 0x0008;
    public static int AC_FINAL        = 0x0010;
    public static int AC_SUPER        = 0x0020;
    public static int AC_SYNCHRONIZED = 0x0020;
    public static int AC_VOLATILE     = 0x0040;
    public static int AC_BRIDGE       = 0x0040;
    public static int AC_VARARGS      = 0x0080;
    public static int AC_TRANSIENT    = 0x0080;
    public static int AC_NATIVE       = 0x0100;
    public static int AC_INTERFACE    = 0x0200;
    public static int AC_ABSTRACT     = 0x0400;
    public static int AC_STRICT       = 0x0800;
    public static int AC_SYNTHETIC    = 0x1000;
    public static int AC_ANNOTATION   = 0x2000;
    public static int AC_ENUM         = 0x4000;

    private SpyInstance instance;


	public SpyLib(SpyInstance instance) {
        this.instance = instance;
	}


    public void add(SpyDefinition...sdefs) {
        for (SpyDefinition sdef : sdefs) {
            instance.add(sdef);
        }
    }


    public SpyDefinition instance() {
        return SpyDefinition.instance();
    }


    public SpyDefinition instrument() {
        return SpyDefinition.instrument().onSubmit().timeDiff(0,1,1).onEnter();
    }


    // TODO instrument(String expr) convenience function;
    public SpyDefinition instrument(String mbsName, String mbeanName, String attrName, String expr) {

        List<Integer> argList = new ArrayList<Integer>();

        Matcher m = ObjectInspector.reVarSubstPattern.matcher(expr);

        // Find out all used arguments
        while (m.find()) {
            String[] segs = m.group(1).split("\\.");
            if (segs[0].matches("^[0-9]+$")) {
                Integer arg = Integer.parseInt(segs[0]);
                if (!argList.contains(arg)) {
                    argList.add(arg);
                }
            }
        }

        // Patch expression string to match argList data
        StringBuffer sb = new StringBuffer(expr.length()+4);
        m = ObjectInspector.reVarSubstPattern.matcher(expr);

        while (m.find()) {
            String[] segs = m.group(1).split("\\.");
            if (segs[0].matches("^[0-9]+$")) {
                segs[0] = ""+argList.indexOf(Integer.parseInt(segs[0]));
                m.appendReplacement(sb, "\\${" + ZorkaUtil.join(".", segs) + "}");
            }
        }

        m.appendTail(sb);


        // Create and return spy definition

        int tidx = argList.size();

        return SpyDefinition.instance()
                .onEnter(argList.toArray()).withTime()
                .onReturn().withTime().onError().withTime()
                .onSubmit().timeDiff(tidx, tidx+1, tidx+1)
                .toStats(mbsName, mbeanName, attrName, sb.toString(), tidx, tidx+1);
    }

}
