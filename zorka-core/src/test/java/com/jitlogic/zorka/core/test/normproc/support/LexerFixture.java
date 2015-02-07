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
package com.jitlogic.zorka.core.test.normproc.support;

import com.jitlogic.zorka.core.normproc.Token;

import java.util.Arrays;
import java.util.List;

import static com.jitlogic.zorka.core.normproc.XqlLexer.*;

public class LexerFixture {
    public Token l(String content) {
        return new Token(T_LITERAL, content);
    }

    public Token k(String content) {
        return new Token(T_KEYWORD, content);
    }

    public Token o(String content) {
        return new Token(T_OPERATOR, content);
    }

    public Token p(String content) {
        return new Token(T_PLACEHOLDER, content);
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

}
