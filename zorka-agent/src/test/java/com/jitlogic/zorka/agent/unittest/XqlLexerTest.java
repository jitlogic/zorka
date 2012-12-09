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
package com.jitlogic.zorka.agent.unittest;

import com.jitlogic.zorka.normproc.Token;
import com.jitlogic.zorka.normproc.XqlLexer;

import static com.jitlogic.zorka.normproc.NormLib.*;
import static com.jitlogic.zorka.normproc.XqlLexer.*;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class XqlLexerTest {

    public Token l(String content) {
        return new Token(T_LITERAL, content);
    }

    public Token k(String content) {
        return new Token(T_KEYWORD, content);
    }

    public Token o(String content) {
        return new Token(T_OPERATOR, content);
    }

    public Token s(String content) {
        return new Token(T_SYMBOL, content);
    }

    public Token w(String content) {
        return new Token(T_WHITESPACE, content);
    }


    public List<Token> lst(Token...tokens) {
        return Arrays.asList(tokens);
    }

    public List<Token> lex(String input) {
        List<Token> tokens = new ArrayList<Token>();
        for (Token token : new XqlLexer(DIALECT_SQL99, input)) {
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
        //assertEquals(lst(l("'a\\'b'")), lex("'a\\'b'")); this is NOT part of SQL-99 standard
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
        assertEquals(lst(o("+"),o("+")), lex("++"));
        assertEquals(lst(s("a"),o("."),s("b")), lex("a.b"));
    }

    @Test
    public void testLexSimpleStatememts() {
        assertEquals(
            lst(k("select"), w(" "), o("*"), w(" "), k("from"), w(" "), s("mytab"), w(" "),
                k("where"), w(" "), s("myfield"), w(" "), o("="), w(" "), l("'abc'")),
            lex("select * from mytab where myfield = 'abc'"));
    }
}
