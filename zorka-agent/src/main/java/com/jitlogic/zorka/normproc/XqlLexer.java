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

    // SQL/HQL dialects


    // More dialects on the way
    //public final static int D_SQL92
    //public final static int D_SQL2003
    //public final static int D_PGSQL
    //public final static int D_MYSQL
    //public final static int D_PLSQL
    //public final static int D_DB2QL
    //public final static int D_HQL
    //public final static int D_EQL

    public final static int T_UNKNOWN     = 0;
    public final static int T_WHITESPACE  = 1;
    public final static int T_SYMBOL      = 2;
    public final static int T_OPERATOR    = 3;
    public final static int T_LITERAL     = 4;
    public final static int T_COMMENT     = 5;
    public final static int T_KEYWORD     = 6;
    public final static int T_PLACEHOLDER = 7;

    // Character table definitions

    public final static int CH_UNKNOWN    = 0;
    public final static int CH_WHITESPACE = 1;
    public final static int CH_SYMSTART   = 2;
    public final static int CH_DIGIT      = 3;
    public final static int CH_OPERATOR   = 4;
    public final static int CH_MINUS      = 5;
    public final static int CH_DOT        = 6;
    public final static int CH_STRDELIM   = 7;
    public final static int CH_STRQUOTE   = 8;
    public final static int CH_PLUS       = 9;
    public final static int CH_CHAR_E     = 10;


    private final static List<String> operatorsSets =
        Collections.unmodifiableList(Arrays.asList(
                "!%&()*,/:;<=>?@[]^"    // D_SQL
        ));


    private static byte[] initChTab(int dialect) {
        String operators = operatorsSets.get(dialect);
        byte[] tab = new byte[128];

        for (int i = 0; i < 128; i++) {
            if (Character.isWhitespace(i)) tab[i]               = CH_WHITESPACE;
            else if ((char)i == 'E') tab[i]                     = CH_CHAR_E;
            else if (Character.isJavaIdentifierStart(i)) tab[i] = CH_SYMSTART;
            else if (Character.isDigit(i)) tab[i]               = CH_DIGIT;
            else if (operators.contains(""+((char)i))) tab[i]   = CH_OPERATOR;
            else if ((char)i == '-') tab[i]                     = CH_MINUS;
            else if ((char)i == '.') tab[i]                     = CH_DOT;
            else if ((char)i == '\'') tab[i]                    = CH_STRDELIM;
            else if ((char)i == '\\') tab[i]                    = CH_STRQUOTE;
            else if ((char)i == '+') tab[i]                     = CH_PLUS;
            else tab[i] = CH_UNKNOWN;
        }

        return tab;
    }


    private final static byte[] CHT_SQL_99 = initChTab(DIALECT_SQL99);


    private final static int S_START = 0;      // Starting point
    private final static int S_WHITESPACE = 1;
    private final static int S_SYMBOL     = 2;
    private final static int S_OPERATOR   = 3;
    private final static int S_INTEGER    = 4;
    private final static int S_FLOAT      = 5;
    private final static int S_STRING     = 6;
    private final static int S_SQUOTE     = 7;  // Possible end of string
    private final static int S_FLOAT_E    = 8;  // Floating point literal in expotential notation
    private final static int S_KEYWORD    = 9;

    private final static int[] tokenTypes = {
            T_UNKNOWN,      // S_START
            T_WHITESPACE,   // S_WHITESPACE
            T_SYMBOL,       // S_SYMBOL
            T_OPERATOR,     // S_OPERATOR
            T_LITERAL,      // S_INTEGER
            T_LITERAL,      // S_FLOAT
            T_LITERAL,      // S_STRING
            T_LITERAL,      // S_SQUOTE
            T_LITERAL,      // S_FLOAT_E
            T_KEYWORD,      // S_KEYWORD
    };

    private final static byte[][][] lextabs = {
            new byte[][] {                                              // DIALECT_SQL99
                           //         UN WS SY DI OP -  .  '  \  +  E
                    lxtab(CHT_SQL_99, E, 1, 2, 4, 3, 3, 3, 6, E, 3, 2), // 0 = S_START
                    lxtab(CHT_SQL_99, E, 1, E, E, E, E, E, E, E, E, E), // 1 = S_WHITESPACE
                    lxtab(CHT_SQL_99, E, E, 2, 2, E, E, E, E, E, E, 2), // 2 = S_SYMBOL
                    lxtab(CHT_SQL_99, E, E, E, 4, E, E, 3, E, E, E, E), // 3 = S_OPERATOR
                    lxtab(CHT_SQL_99, E, E, E, 4, E, E, 5, E, E, E, E), // 4 = S_INTEGER
                    lxtab(CHT_SQL_99, E, E, E, 5, E, E, E, E, E, E, 8), // 5 = S_FLOAT
                    lxtab(CHT_SQL_99, 6, 6, 6, 6, 6, 6, 6, 7, 6, 6, 6), // 6 = S_STRING
                    lxtab(CHT_SQL_99, E, E, E, E, E, E, E, 6, E, E, E), // 7 = S_SQUOTE
                    lxtab(CHT_SQL_99, E, E, E, 8, E, E, E, E, E, 8, E), // 8 = S_FLOAT_E
            },
    };

    // Keyword sets

    private final static Set<String> SQL_99_KEYWORDS = strSet(
            "ABSOLUTE", "ACTION", "ADD", "AFTER", "ALL", "ALLOCATE", "ALTER", "AND", "ANY", "ARE",
            "ARRAY", "AS", "ASC", "ASENSITIVE", "ASSERTION", "ASYMMETRIC", "AT", "ATOMIC", "AUTHORIZATION",
            "BEFORE", "BEGIN", "BETWEEN", "BINARY", "BIT", "BLOB", "BOOLEAN", "BOTH", "BREADTH", "BY", "CALL",
            "CASCADE", "CASCADED", "CASE", "CAST", "CATALOG", "CHAR", "CHARACTER", "CHECK", "CLOB", "CLOSE",
            "COLLATE", "COLLATION", "COLUMN", "COMMIT", "CONDITION", "CONNECT", "CONNECTION", "CONSTRAINT",
            "CONSTRAINTS", "CONSTRUCTOR", "CONTINUE", "CORRESPONDING", "CREATE", "CROSS", "CUBE", "CURRENT",
            "CURRENT_DATE", "CURRENT_DEFAULT_TRANSFORM_GROUP", "CURRENT_PATH", "CURRENT_ROLE", "CURRENT_TIME",
            "CURRENT_TIMESTAMP", "CURRENT_TRANSFORM_GROUP_FOR_TYPE", "CURRENT_USER", "CURSOR", "CYCLE",
            "DATA", "DATE", "DAY", "DEALLOCATE", "DEC", "DECIMAL", "DECLARE", "DEFAULT", "DEFERRABLE", "DEFERRED",
            "DELETE", "DEPTH", "DEREF", "DESC", "DESCRIBE", "DESCRIPTOR", "DETERMINISTIC", "DIAGNOSTICS",
            "DISCONNECT", "DISTINCT", "DO", "DOMAIN", "DOUBLE", "DROP", "DYNAMIC", "EACH", "ELSE", "ELSEIF",
            "END", "EQUALS", "ESCAPE", "EXCEPT", "EXCEPTION", "EXEC", "EXECUTE", "EXISTS", "EXIT", "EXTERNAL",
            "FALSE", "FETCH", "FILTER", "FIRST", "FLOAT", "FOR", "FOREIGN", "FOUND", "FREE", "FROM", "FULL",
            "FUNCTION", "GENERAL", "GET", "GLOBAL", "GO", "GOTO", "GRANT", "GROUP", "GROUPING", "HANDLER",
            "HAVING", "HOLD", "HOUR", "IDENTITY", "IF", "IMMEDIATE", "IN", "INDICATOR", "INITIALLY", "INNER",
            "INOUT", "INPUT", "INSENSITIVE", "INSERT", "INT", "INTEGER", "INTERSECT", "INTERVAL", "INTO", "IS",
            "ISOLATION", "ITERATE", "JOIN", "KEY", "LANGUAGE", "LARGE", "LAST", "LATERAL", "LEADING", "LEAVE",
            "LEFT", "LEVEL", "LIKE", "LOCAL", "LOCALTIME", "LOCALTIMESTAMP", "LOCATOR", "LOOP", "MAP", "MATCH",
            "METHOD", "MINUTE", "MODIFIES", "MODULE", "MONTH", "NAMES", "NATIONAL", "NATURAL", "NCHAR", "NCLOB",
            "NEW", "NEXT", "NO", "NONE", "NOT", "NULL", "NUMERIC", "OBJECT", "OF", "OLD", "ON", "ONLY", "OPEN",
            "OPTION", "OR", "ORDER", "ORDINALITY", "OUT", "OUTER", "OUTPUT", "OVER", "OVERLAPS", "PAD", "PARAMETER",
            "PARTIAL", "PARTITION", "PATH", "PRECISION", "PREPARE", "PRESERVE", "PRIMARY", "PRIOR", "PRIVILEGES",
            "PROCEDURE", "PUBLIC", "RANGE", "READ", "READS", "REAL", "RECURSIVE", "REF", "REFERENCES", "REFERENCING",
            "RELATIVE", "RELEASE", "REPEAT", "RESIGNAL", "RESTRICT", "RESULT", "RETURN", "RETURNS", "REVOKE",
            "RIGHT", "ROLE", "ROLLBACK", "ROLLUP", "ROUTINE", "ROW", "ROWS", "SAVEPOINT", "SCHEMA", "SCOPE",
            "SCROLL", "SEARCH", "SECOND", "SECTION", "SELECT", "SENSITIVE", "SESSION", "SESSION_USER", "SET",
            "SETS", "SIGNAL", "SIMILAR", "SIZE", "SMALLINT", "SOME", "SPACE", "SPECIFIC", "SPECIFICTYPE",
            "SQL", "SQLEXCEPTION", "SQLSTATE", "SQLWARNING", "START", "STATE", "STATIC", "SYMMETRIC", "SYSTEM",
            "SYSTEM_USER", "TABLE", "TEMPORARY", "THEN", "TIME", "TIMESTAMP", "TIMEZONE_HOUR", "TIMEZONE_MINUTE",
            "TO", "TRAILING", "TRANSACTION", "TRANSLATION", "TREAT", "TRIGGER", "TRUE", "UNDER", "UNDO", "UNION",
            "UNIQUE", "UNKNOWN", "UNNEST", "UNTIL", "UPDATE", "USAGE", "USER", "USING", "VALUE", "VALUES", "VARCHAR",
            "VARYING", "VIEW", "WHEN", "WHENEVER", "WHERE", "WHILE", "WINDOW", "WITH", "WITHIN", "WITHOUT", "WORK",
            "WRITE", "YEAR", "ZONE");


    public static final List<Set<String>> keywordSets = Collections.unmodifiableList(Arrays.asList(
            SQL_99_KEYWORDS
    ));


    protected Set<String> keywordSet;


    public XqlLexer(int dialect, String input) {
        super(input, lextabs[dialect]);
        this.keywordSet = keywordSets.get(dialect);
    }


    public Token next() {
        Token token = super.next();

        int type = token.getType();

        if (type == S_SYMBOL && keywordSet.contains(token.getContent().toLowerCase())) {
            type = S_KEYWORD;
        }

        token.setType(tokenTypes[type]);

        return token;
    }

}
