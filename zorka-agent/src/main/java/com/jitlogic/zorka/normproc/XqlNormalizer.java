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
package com.jitlogic.zorka.normproc;

import static com.jitlogic.zorka.normproc.XqlLexer.*;

public class XqlNormalizer implements Normalizer {

    private static final boolean T = true, F = false;
    private static Token PHD_TOKEN = new Token(T_PLACEHOLDER, "?");
    // U  W  S  O  L  C  K  P

    private static boolean[][] joints = {
           // U  W  S  O  L  C  K  P
            { F, F, F, F, F, F, F, F }, // U (UNKNOWN)
            { F, F, F, F, F, F, F, F }, // W (WHITESPACE)
            { F, F, T, F, T, F, T, F }, // S (SYMBOL)
            { F, F, F, F, F, F, T, F }, // O (OPERATOR)
            { F, F, T, F, T, F, T, F }, // L (LITERAL)
            { F, F, F, F, F, F, F, F }, // C (COMMENT)
            { F, F, T, T, T, F, T, F }, // K (KEYWORD)
            { F, F, F, F, F, F, F, F }, // P (PLACEHOLDER)
    };

    private static boolean[] fcut = { T, T, F, F, F, T, F, F }; // Tokens to be cut off;
    private static boolean[] fphd = { F, F, F, F, T, F, F, F }; // Tokens to be replaced by placeholders;
    private static boolean[] fcas = { F, F, T, F, F, F, T, F }; // Tokens to be converted to upper (lower) case;

    private int dialect, defaultFlags;
    private boolean upcase = false;

    public XqlNormalizer(int dialect, int defaultFlags) {
        this.dialect = dialect;
        this.defaultFlags = defaultFlags;
    }


    public String normalize(String input) {

        if (input == null) {
            return null;
        }

        StringBuffer sb = new StringBuffer(input.length()+2);
        XqlLexer lexer = new XqlLexer(dialect, input);
        int last = T_UNKNOWN;

        while (lexer.hasNext()) {
            Token token = lexer.next();
            int t = token.getType();
            String s = token.getContent();

            if (0 != (defaultFlags & (1<<t))) {
                if (fcut[t]) { continue; }
                if (fphd[t]) { token = PHD_TOKEN; }
                if (fcas[t]) { token = new Token(t, upcase ? s.toUpperCase() : s.toLowerCase()); }
            }

            if (joints[last][t]) {
                sb.append(" ");
            }

            sb.append(token.getContent());
            last = t;
        }

        return sb.toString();
    }

}
