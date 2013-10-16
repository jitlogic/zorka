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


import com.jitlogic.zico.core.eql.ast.EqlExpression;
import com.jitlogic.zorka.common.util.ZorkaUtil;
import org.parboiled.Parboiled;
import org.parboiled.parserunners.ReportingParseRunner;
import org.parboiled.support.ParsingResult;

import java.util.Map;

public class Parser {

    private static final ParserSyntax rules = Parboiled.createParser(ParserSyntax.class);

    public static EqlExpression expr(String expr) throws ParseException {
        ParsingResult<EqlExpression> rslt = new ReportingParseRunner(rules.Expression()).run(expr);

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

}
