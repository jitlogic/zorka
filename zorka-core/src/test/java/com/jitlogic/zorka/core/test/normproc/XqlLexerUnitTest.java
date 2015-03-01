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
import com.jitlogic.zorka.core.normproc.Token;
import com.jitlogic.zorka.core.normproc.XqlLexer;

import static com.jitlogic.zorka.core.normproc.NormLib.*;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

public class XqlLexerUnitTest extends LexerFixture {

    public List<Token> lex(String input) {
        return lex(DIALECT_SQL_99, input);
    }

    public List<Token> lex(int dialect, String input) {
        List<Token> tokens = new ArrayList<Token>();
        for (Token token : new XqlLexer(dialect, input)) {
            tokens.add(token);
        }
        return tokens;
    }


    @Test
    public void testLexBasicTokens() {
        assertEquals(lst(w("   ")), lex("   "));
    }


    @Test
    public void testLexSymbolsAndKeywords() {
        assertEquals(lst(s("somevar")), lex("somevar"));
        assertEquals(lst(s("a"), w(" "), s("b")), lex("a b"));
        assertEquals(lst(k("select")), lex("select"));
    }


    @Test
    public void testLexIntegerLiterals() {
        assertEquals(lst(l("1")), lex("1"));
        assertEquals(lst(l("-1")), lex("-1"));
    }


    @Test
    public void testLexStringLiterals() {
        assertEquals(lst(l("'abc'")), lex("'abc'"));
        assertEquals(lst(l("'a''b'")), lex("'a''b'"));
    }


    @Test
    public void testLexFloatLiterals() {
        assertEquals(lst(l("0.1")), lex("0.1"));
        assertEquals(lst(l("-0.1")), lex("-0.1"));
        assertEquals(lst(l(".1")), lex(".1"));
        assertEquals(lst(l("-.1")), lex("-.1"));
        assertEquals(lst(l("1.0E+3")), lex("1.0E+3"));
    }


    @Test
    public void testLexOperators() {
        assertEquals(lst(o("+")), lex("+"));
        assertEquals(lst(o("+"), o("+")), lex("++"));
        assertEquals(lst(s("a"), o("."), s("b")), lex("a.b"));
    }


    @Test
    public void testLexQuotedIdentifier() {
        assertEquals(lst(s("\"select\"")), lex("\"select\""));
        assertEquals(lst(w(" "), s("\"select\""), w(" ")), lex(" \"select\" "));
    }

    @Test
    public void testLexQparam() {
        assertEquals(lst(p("?")), lex("?"));
        assertEquals(lst(k("where"), w(" "), p("?"), w(" "), o("="), w(" "), l("2")), lex("where ? = 2"));
    }

    @Test
    public void testLexNParam() {
        assertEquals(lst(p(":abc")), lex(":abc"));
        assertEquals(lst(k("where"), w(" "), p(":12"), w(" "), o("="), w(" "), l("1")), lex("where :12 = 1"));
    }

    @Test
    public void testLexMssqlQuotedIdentifier() {
        assertEquals(lst(s("[select]")), lex(DIALECT_MSSQL, "[select]"));
        assertEquals(lst(w(" "), s("[select]"), w(" ")), lex(DIALECT_MSSQL, " [select] "));
    }


    @Test
    public void testLexSimpleStatememts() {
        assertEquals(
            lst(k("select"), w(" "), o("*"), w(" "), k("from"), w(" "), s("mytab"), w(" "),
                    k("where"), w(" "), s("myfield"), w(" "), o("="), w(" "), l("'abc'")),
            lex("select * from mytab where myfield = 'abc'"));
    }

    @Test
    public void testXqlKeywordSets() {
        for (Set<String> kwset : XqlLexer.keywordSets) {

        }
    }
}
