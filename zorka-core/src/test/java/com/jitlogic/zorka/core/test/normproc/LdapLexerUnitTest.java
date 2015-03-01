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
package com.jitlogic.zorka.core.test.normproc;

import com.jitlogic.zorka.core.test.normproc.support.LexerFixture;
import com.jitlogic.zorka.core.normproc.LdapLexer;
import com.jitlogic.zorka.core.normproc.Token;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

public class LdapLexerUnitTest extends LexerFixture {

    public List<Token> lex(String input) {
        List<Token> tokens = new ArrayList<Token>();
        for (Token token : new LdapLexer(input)) {
            tokens.add(token);
        }
        return tokens;
    }


    @Test
    public void testLexWhitespaces() {
        assertEquals(lst(), lex(""));
        assertEquals(lst(w("  ")), lex("  "));
    }


    @Test
    public void testLexEmptyFilter() {
        assertEquals(lst(o("("), o(")")), lex("()"));
    }


    @Test
    public void testLexSimpleFilters() {
        assertEquals(
                lst(o("("),s("cn"),o("="),l("buba"),o(")")),
                lex("(cn=buba)"));

        assertEquals(
                lst(o("("),s("cn"),o("="),l("bu ba"),o(")")),
                lex("(cn=bu ba)"));

        assertEquals(
                lst(w(" "), o("("),s("cn"),o("="),l("bu ba"),o(")")),
                lex(" (cn=bu ba)"));
    }


    @Test
    public void testLexCompositeFilters() {
        assertEquals(
                lst(o("("), o("&"), o("("), s("a"), o("="), l("b"), o(")"),
                        o("("), s("c"), o("="), l("d"), o(")"), o(")")),
                lex("(&(a=b)(c=d))"));
    }
}
