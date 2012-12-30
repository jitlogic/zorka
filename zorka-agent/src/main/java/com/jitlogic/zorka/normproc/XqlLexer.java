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

import com.jitlogic.zorka.util.ZorkaUtil;

import java.util.*;

import static com.jitlogic.zorka.normproc.NormLib.*;

/**
 * This is simplified DFA lexer for all *QL dialects. It is not very accurate,
 * so it can produce garbage in certain cases (albeit in predictable way ;) ), but for
 * query normalization purposes it is sufficient. On the other hand it should be pretty
 * fast as lexer does not perform backtracking.
 *
 * @author Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
 *
 */
public class XqlLexer extends Lexer {

    // Character table definitions

    public static final byte CH_UNKNOWN    = 0;
    public static final byte CH_WHITESPACE = 1;
    public static final byte CH_SYMSTART   = 2;
    public static final byte CH_DIGIT      = 3;
    public static final byte CH_OPERATOR   = 4;
    public static final byte CH_MINUS      = 5;
    public static final byte CH_DOT        = 6;
    public static final byte CH_STRDELIM   = 7;
    public static final byte CH_STRQUOTE   = 8;
    public static final byte CH_PLUS       = 9;
    public static final byte CH_CHAR_E     = 10;
    public static final byte CH_IDQUOTE    = 11;
    public static final byte CH_QMARK      = 12;
    public static final byte CH_COLON      = 13;

    //private final static int S_WHITESPACE = 1;  // white space
    //private final static int S_SYMBOL     = 2;  // identifiers
    //private final static int S_OPERATOR   = 3;  // operators
    //private final static int S_INTEGER    = 4;  // integer literal
    //private final static int S_FLOAT      = 5;  // floating point number literal
    //private final static int S_STRING     = 6;  // string
    //private final static int S_SQUOTE     = 7;  // possible end of string or quoted character
    //private final static int S_FLOAT_E    = 8;  // floating point literal in expotential notation
    //private final static int S_QPARAM     = 9;  // unnamed query parameter (starting with '?')
    //private final static int S_NPARAM     = 10; // named query parameter (starting with ':')
    //private final static int S_KEYWORD    = 11; // keywords


    private static byte[] initChTab(String operators, Map<Character, Byte> chmap) {
        byte[] tab = new byte[128];

        for (int i = 0; i < 128; i++) {
            char ch = (char)i;
            if (chmap.containsKey(ch)) tab[i] = chmap.get(ch);
            else if (Character.isWhitespace(i)) tab[i]               = CH_WHITESPACE;
            else if (Character.isJavaIdentifierStart(i)) tab[i] = CH_SYMSTART;
            else if (Character.isDigit(i)) tab[i]               = CH_DIGIT;
            else if (operators.contains(""+((char)i))) tab[i]   = CH_OPERATOR;
            else tab[i] = CH_UNKNOWN;
        }

        return tab;
    }

    private static final int[] tokenTypes = {
            T_UNKNOWN,
            T_WHITESPACE,
            T_SYMBOL,
            T_OPERATOR,
            T_LITERAL,
            T_LITERAL,
            T_LITERAL,
            T_LITERAL,
            T_LITERAL,
            T_PLACEHOLDER,
            T_PLACEHOLDER,
            T_KEYWORD,
    };

    private static final Map<Character,Byte> CHM_SQL = ZorkaUtil.map(
            'E', CH_CHAR_E, '-', CH_MINUS, '.', CH_DOT, '\'', CH_STRDELIM,
            '\\', CH_STRQUOTE, '+', CH_PLUS, '"', CH_IDQUOTE, '?', CH_QMARK,
            ':', CH_COLON
    );

    private static final byte[] CHT_SQL = initChTab("!%&()*,/;<=>@[]^", CHM_SQL);

    private static final byte[][] LEX_SQL = {
                   //      UN WS SY DI OP -  .  '  \  +  E  "  ?  :
            lxtab(CHT_SQL, E, 1, 2, 4, 3, 3, 3, 6, E, 3, 2, 2, 9,10),
            lxtab(CHT_SQL, E, 1, E, E, E, E, E, E, E, E, E, E, E, E),
            lxtab(CHT_SQL, E, E, 2, 2, E, E, E, E, E, E, 2, 2, E, E),
            lxtab(CHT_SQL, E, E, E, 4, E, E, 3, E, E, E, E, E, E, E),
            lxtab(CHT_SQL, E, E, E, 4, E, E, 5, E, E, E, E, E, E, E),
            lxtab(CHT_SQL, E, E, E, 5, E, E, E, E, E, E, 8, E, E, E),
            lxtab(CHT_SQL, 6, 6, 6, 6, 6, 6, 6, 7, 6, 6, 6, 6, 6, 6),
            lxtab(CHT_SQL, E, E, E, E, E, E, E, 6, E, E, E, E, E, E),
            lxtab(CHT_SQL, E, E, E, 8, E, E, E, E, E, 8, E, 2, E, E),
            lxtab(CHT_SQL, E, E, E, E, E, E, E, E, E, E, E, E, E, E),
            lxtab(CHT_SQL, E, E,10,10, E, E, E, E, E, E, E, E, E, E),
    };

    private static final Map<Character,Byte> CHM_MYSQL = ZorkaUtil.map(
            'E', CH_CHAR_E, '-', CH_MINUS, '.', CH_DOT, '\'', CH_STRDELIM,
            '\\', CH_STRQUOTE, '+', CH_PLUS, '`', CH_IDQUOTE, '?', CH_QMARK,
            ':', CH_COLON
    );

    private static final byte[] CHT_MYSQL = initChTab("!%&()*,/;<=>@[]^", CHM_MYSQL);

    private static final byte[][] LEX_MYSQL = {
                     //     UN WS SY DI OP -   .  '  \  +  E  "  ?  :
            lxtab(CHT_MYSQL, E, 1, 2, 4, 3, 3, 3, 6, E, 3, 2, 2, 9,10),
            lxtab(CHT_MYSQL, E, 1, E, E, E, E, E, E, E, E, E, E, E, E),
            lxtab(CHT_MYSQL, E, E, 2, 2, E, E, E, E, E, E, 2, 2, E, E),
            lxtab(CHT_MYSQL, E, E, E, 4, E, E, 3, E, E, E, E, E, E, E),
            lxtab(CHT_MYSQL, E, E, E, 4, E, E, 5, E, E, E, E, E, E, E),
            lxtab(CHT_MYSQL, E, E, E, 5, E, E, E, E, E, E, 8, E, E, E),
            lxtab(CHT_MYSQL, 6, 6, 6, 6, 6, 6, 6, 7, 6, 6, 6, 6, 6, 6),
            lxtab(CHT_MYSQL, E, E, E, E, E, E, E, 6, E, E, E, E, E, E),
            lxtab(CHT_MYSQL, E, E, E, 8, E, E, E, E, E, 8, E, 2, E, E),
            lxtab(CHT_MYSQL, E, E, E, E, E, E, E, E, E, E, E, E, E, E),
            lxtab(CHT_MYSQL, E, E,10,10, E, E, E, E, E, E, E, E, E, E),
    };

    private static final Map<Character,Byte> CHM_MSSQL = ZorkaUtil.map(
            'E', CH_CHAR_E, '-', CH_MINUS, '.', CH_DOT, '?', CH_QMARK,
            '\'', CH_STRDELIM, '\\', CH_STRQUOTE, '+', CH_PLUS,
            '`', CH_IDQUOTE, '[', CH_IDQUOTE, ']', CH_IDQUOTE,
            '?', CH_QMARK, ':', CH_COLON
    );

    private static final byte[] CHT_MSSQL = initChTab("!%&()*,/;<=>@^", CHM_MSSQL);

    private static final byte[][] LEX_MSSQL = {
                     //     UN WS SY DI OP  -  .  '  \  +  E  "  ?  :
            lxtab(CHT_MSSQL, E, 1, 2, 4, 3, 3, 3, 6, E, 3, 2, 2, 9,10),
            lxtab(CHT_MSSQL, E, 1, E, E, E, E, E, E, E, E, E, E, E, E),
            lxtab(CHT_MSSQL, E, E, 2, 2, E, E, E, E, E, E, 2, 2, E, E),
            lxtab(CHT_MSSQL, E, E, E, 4, E, E, 3, E, E, E, E, E, E, E),
            lxtab(CHT_MSSQL, E, E, E, 4, E, E, 5, E, E, E, E, E, E, E),
            lxtab(CHT_MSSQL, E, E, E, 5, E, E, E, E, E, E, 8, E, E, E),
            lxtab(CHT_MSSQL, 6, 6, 6, 6, 6, 6, 6, 7, 6, 6, 6, 6, 6, 6),
            lxtab(CHT_MSSQL, E, E, E, E, E, E, E, 6, E, E, E, E, E, E),
            lxtab(CHT_MSSQL, E, E, E, 8, E, E, E, E, E, 8, E, 2, E, E),
            lxtab(CHT_MSSQL, E, E, E, E, E, E, E, E, E, E, E, E, E, E),
            lxtab(CHT_MSSQL, E, E,10,10, E, E, E, E, E, E, E, E, E, E),
    };

    // Keyword sets


    private static final byte[][][] lextabs = {
            LEX_SQL,
            LEX_SQL,
            LEX_SQL,
            LEX_MSSQL,
            LEX_SQL,
            LEX_MYSQL,
            LEX_SQL,
            LEX_SQL,
            LEX_SQL,
            LEX_SQL
    };


    private static Map<String,Set<String>> KW_MAPS = readKeywordFile("/com/jitlogic/zorka/normproc/xqldialects.properties");

    public static final List<Set<String>> keywordSets = Collections.unmodifiableList(Arrays.asList(
            KW_MAPS.get("SQL_92_KEYWORDS"),
            KW_MAPS.get("SQL_99_KEYWORDS"),
            KW_MAPS.get("SQL_2003_KEYWORDS"),
            KW_MAPS.get("MSSQL_KEYWORDS"),
            KW_MAPS.get("PGSQL_KEYWORDS"),
            KW_MAPS.get("MYSQL_KEYWORDS"),
            KW_MAPS.get("DB2_KEYWORDS"),
            KW_MAPS.get("ORACLE_KEYWORDS"),
            KW_MAPS.get("HQL_KEYWORDS"),
            KW_MAPS.get("JPA_KEYWORDS")
    ));



    protected Set<String> keywordSet;
    private int dialect;

    public XqlLexer(int dialect, String input) {
        super(input, lextabs[dialect], tokenTypes);
        this.keywordSet = keywordSets.get(dialect);
        this.dialect = dialect;
    }


    public Token next() {
        Token token = super.next();

        if (token.getType() == T_SYMBOL && keywordSet.contains(token.getContent().toLowerCase())) {
            token.setType(T_KEYWORD);
        }

        return token;
    }

    @Override
    public Lexer lex(String input) {
        return new XqlLexer(dialect, input);
    }

}
