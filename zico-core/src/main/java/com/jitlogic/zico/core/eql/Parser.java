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
package com.jitlogic.zico.core.eql;


import com.jitlogic.zico.core.eql.ast.EqlExpr;
import com.jitlogic.zorka.common.util.ZorkaUtil;
import org.parboiled.Parboiled;
import org.parboiled.parserunners.ReportingParseRunner;
import org.parboiled.support.ParsingResult;

import java.util.Map;

public class Parser {

    private static final ParserSyntax rules = Parboiled.createParser(ParserSyntax.class);

    public static EqlExpr expr(String expr) throws ParseException {
        ParsingResult<EqlExpr> rslt = new ReportingParseRunner(rules.SoleExpression()).run(expr);

        if (rslt.hasErrors() || !rslt.matched) {
            throw new ParseException("Error parsing EQL expression", rslt.parseErrors);
        }

        return rslt.resultValue;
    }

    private static final Map<Character, Character> ESCAPE_CHARS = ZorkaUtil.map(
            'n', '\n', 'b', '\b', 't', '\t', 'r', '\r', 'f', '\f');

    public static String unescape(String s) {
        StringBuilder sb = new StringBuilder(s.length());

        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == '\\') {
                char c = s.charAt(i + 1);
                sb.append(ESCAPE_CHARS.containsKey(c) ? ESCAPE_CHARS.get(c) : c);
                i++;
            } else {
                sb.append(s.charAt(i));
            }
        }

        return sb.toString();
    }

    public static Object extendMap(Object map, String key, Object val) {
        ((Map) map).put(key, val);
        return map;
    }

    private static final Map<String, Long> TIME_SUFFIXES = ZorkaUtil.map(
            "ns", 1L,
            "us", 1000L,
            "ms", 1000000L,
            "s", 1000000000L,
            "m", 60000000000L,
            "h", 3600000000000L
    );

    public static long timestamp(Object map, String suffix) {
        Long t1 = (Long) ((Map) map).get("t1"), t2 = (Long) ((Map) map).get("t2");
        long scale = TIME_SUFFIXES.get(suffix), t = t1 * scale;

        if (t2 != null) {
            // No use of floating point to ensure exact remainder calculation
            long tt = scale * t2, dt = 1;
            while (dt < t2) {
                dt *= 10;
            }
            t += tt / dt;
        }

        return t;
    }
}
