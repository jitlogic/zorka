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

    public final static byte CH_UNKNOWN    = 0;
    public final static byte CH_WHITESPACE = 1;
    public final static byte CH_SYMSTART   = 2;
    public final static byte CH_DIGIT      = 3;
    public final static byte CH_OPERATOR   = 4;
    public final static byte CH_MINUS      = 5;
    public final static byte CH_DOT        = 6;
    public final static byte CH_STRDELIM   = 7;
    public final static byte CH_STRQUOTE   = 8;
    public final static byte CH_PLUS       = 9;
    public final static byte CH_CHAR_E     = 10;
    public final static byte CH_IDQUOTE    = 11;
    public final static byte CH_QMARK      = 12;
    public final static byte CH_COLON      = 13;

    private final static int S_WHITESPACE = 1;  // white space
    private final static int S_SYMBOL     = 2;  // identifiers
    private final static int S_OPERATOR   = 3;  // operators
    private final static int S_INTEGER    = 4;  // integer literal
    private final static int S_FLOAT      = 5;  // floating point number literal
    private final static int S_STRING     = 6;  // string
    private final static int S_SQUOTE     = 7;  // possible end of string or quoted character
    private final static int S_FLOAT_E    = 8;  // floating point literal in expotential notation
    private final static int S_QPARAM     = 9;  // unnamed query parameter (starting with '?')
    private final static int S_NPARAM     = 10; // named query parameter (starting with ':')
    private final static int S_KEYWORD    = 11; // keywords


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
            T_PLACEHOLDER,  // S_QPARAM
            T_PLACEHOLDER,  // S_LPARAM
            T_KEYWORD,      // S_KEYWORD
    };

    private static Map<Character,Byte> CHM_SQL = ZorkaUtil.map(
            'E', CH_CHAR_E, '-', CH_MINUS, '.', CH_DOT, '\'', CH_STRDELIM,
            '\\', CH_STRQUOTE, '+', CH_PLUS, '"', CH_IDQUOTE, '?', CH_QMARK,
            ':', CH_COLON
    );

    private final static byte[] CHT_SQL = initChTab("!%&()*,/;<=>@[]^", CHM_SQL);

    private final static byte[][] LEX_SQL = { // DIALECT_SQL99
                   //      UN WS SY DI OP -  .  '  \  +  E  "  ?  :
            lxtab(CHT_SQL, E, 1, 2, 4, 3, 3, 3, 6, E, 3, 2, 2, 9,10), // 0 = S_START
            lxtab(CHT_SQL, E, 1, E, E, E, E, E, E, E, E, E, E, E, E), // 1 = S_WHITESPACE
            lxtab(CHT_SQL, E, E, 2, 2, E, E, E, E, E, E, 2, 2, E, E), // 2 = S_SYMBOL
            lxtab(CHT_SQL, E, E, E, 4, E, E, 3, E, E, E, E, E, E, E), // 3 = S_OPERATOR
            lxtab(CHT_SQL, E, E, E, 4, E, E, 5, E, E, E, E, E, E, E), // 4 = S_INTEGER
            lxtab(CHT_SQL, E, E, E, 5, E, E, E, E, E, E, 8, E, E, E), // 5 = S_FLOAT
            lxtab(CHT_SQL, 6, 6, 6, 6, 6, 6, 6, 7, 6, 6, 6, 6, 6, 6), // 6 = S_STRING
            lxtab(CHT_SQL, E, E, E, E, E, E, E, 6, E, E, E, E, E, E), // 7 = S_SQUOTE
            lxtab(CHT_SQL, E, E, E, 8, E, E, E, E, E, 8, E, 2, E, E), // 8 = S_FLOAT_E
            lxtab(CHT_SQL, E, E, E, E, E, E, E, E, E, E, E, E, E, E), // 9 = S_QPARAM
            lxtab(CHT_SQL, E, E,10,10, E, E, E, E, E, E, E, E, E, E), // 10 = S_NPARAM
    };

    private static Map<Character,Byte> CHM_MYSQL = ZorkaUtil.map(
            'E', CH_CHAR_E, '-', CH_MINUS, '.', CH_DOT, '\'', CH_STRDELIM,
            '\\', CH_STRQUOTE, '+', CH_PLUS, '`', CH_IDQUOTE, '?', CH_QMARK,
            ':', CH_COLON
    );

    private final static byte[] CHT_MYSQL = initChTab("!%&()*,/;<=>@[]^", CHM_MYSQL);

    private final static byte[][] LEX_MYSQL = { // DIALECT_SQL99
                     //     UN WS SY DI OP -   .  '  \  +  E  "  ?  :
            lxtab(CHT_MYSQL, E, 1, 2, 4, 3, 3, 3, 6, E, 3, 2, 2, 9,10), // 0 = S_START
            lxtab(CHT_MYSQL, E, 1, E, E, E, E, E, E, E, E, E, E, E, E), // 1 = S_WHITESPACE
            lxtab(CHT_MYSQL, E, E, 2, 2, E, E, E, E, E, E, 2, 2, E, E), // 2 = S_SYMBOL
            lxtab(CHT_MYSQL, E, E, E, 4, E, E, 3, E, E, E, E, E, E, E), // 3 = S_OPERATOR
            lxtab(CHT_MYSQL, E, E, E, 4, E, E, 5, E, E, E, E, E, E, E), // 4 = S_INTEGER
            lxtab(CHT_MYSQL, E, E, E, 5, E, E, E, E, E, E, 8, E, E, E), // 5 = S_FLOAT
            lxtab(CHT_MYSQL, 6, 6, 6, 6, 6, 6, 6, 7, 6, 6, 6, 6, 6, 6), // 6 = S_STRING
            lxtab(CHT_MYSQL, E, E, E, E, E, E, E, 6, E, E, E, E, E, E), // 7 = S_SQUOTE
            lxtab(CHT_MYSQL, E, E, E, 8, E, E, E, E, E, 8, E, 2, E, E), // 8 = S_FLOAT_E
            lxtab(CHT_MYSQL, E, E, E, E, E, E, E, E, E, E, E, E, E, E), // 9 = S_QPARAM
            lxtab(CHT_MYSQL, E, E,10,10, E, E, E, E, E, E, E, E, E, E), // 10 = S_NPARAM
    };

    private static Map<Character,Byte> CHM_MSSQL = ZorkaUtil.map(
            'E', CH_CHAR_E, '-', CH_MINUS, '.', CH_DOT, '?', CH_QMARK,
            '\'', CH_STRDELIM, '\\', CH_STRQUOTE, '+', CH_PLUS,
            '`', CH_IDQUOTE, '[', CH_IDQUOTE, ']', CH_IDQUOTE,
            '?', CH_QMARK, ':', CH_COLON
    );

    private final static byte[] CHT_MSSQL = initChTab("!%&()*,/;<=>@^", CHM_MSSQL);

    private final static byte[][] LEX_MSSQL = { // DIALECT_SQL99
                     //     UN WS SY DI OP  -  .  '  \  +  E  "  ?  :
            lxtab(CHT_MSSQL, E, 1, 2, 4, 3, 3, 3, 6, E, 3, 2, 2, 9,10), // 0 = S_START
            lxtab(CHT_MSSQL, E, 1, E, E, E, E, E, E, E, E, E, E, E, E), // 1 = S_WHITESPACE
            lxtab(CHT_MSSQL, E, E, 2, 2, E, E, E, E, E, E, 2, 2, E, E), // 2 = S_SYMBOL
            lxtab(CHT_MSSQL, E, E, E, 4, E, E, 3, E, E, E, E, E, E, E), // 3 = S_OPERATOR
            lxtab(CHT_MSSQL, E, E, E, 4, E, E, 5, E, E, E, E, E, E, E), // 4 = S_INTEGER
            lxtab(CHT_MSSQL, E, E, E, 5, E, E, E, E, E, E, 8, E, E, E), // 5 = S_FLOAT
            lxtab(CHT_MSSQL, 6, 6, 6, 6, 6, 6, 6, 7, 6, 6, 6, 6, 6, 6), // 6 = S_STRING
            lxtab(CHT_MSSQL, E, E, E, E, E, E, E, 6, E, E, E, E, E, E), // 7 = S_SQUOTE
            lxtab(CHT_MSSQL, E, E, E, 8, E, E, E, E, E, 8, E, 2, E, E), // 8 = S_FLOAT_E
            lxtab(CHT_MSSQL, E, E, E, E, E, E, E, E, E, E, E, E, E, E), // 9 = S_QPARAM
            lxtab(CHT_MSSQL, E, E,10,10, E, E, E, E, E, E, E, E, E, E), // 10 = S_NPARAM
    };

    // Keyword sets

    private final static Set<String> SQL_92_KEYWORDS = strSet(
            "ABSOLUTE", "ACTION", "ADD", "ALL", "ALLOCATE", "ALTER", "AND", "ANY", "ARE", "AS", "ASC", "ASSERTION",
            "AT", "AUTHORIZATION", "AVG", "BEGIN", "BETWEEN", "BIT", "BIT_LENGTH", "BOTH", "BY", "CALL", "CASCADE",
            "CASCADED", "CASE", "CAST", "CATALOG", "CHAR", "CHARACTER", "CHARACTER_LENGTH", "CHAR_LENGTH", "CHECK",
            "CLOSE", "COALESCE", "COLLATE", "COLLATION", "COLUMN", "COMMIT", "CONDITION", "CONNECT", "CONNECTION",
            "CONSTRAINT", "CONSTRAINTS", "CONTAINS", "CONTINUE", "CONVERT", "CORRESPONDING", "COUNT", "CREATE",
            "CROSS", "CURRENT", "CURRENT_DATE", "CURRENT_PATH", "CURRENT_TIME", "CURRENT_TIMESTAMP", "CURRENT_USER",
            "CURSOR", "DATE", "DAY", "DEALLOCATE", "DEC", "DECIMAL", "DECLARE", "DEFAULT", "DEFERRABLE", "DEFERRED",
            "DELETE", "DESC", "DESCRIBE", "DESCRIPTOR", "DETERMINISTIC", "DIAGNOSTICS", "DISCONNECT", "DISTINCT",
            "DO", "DOMAIN", "DOUBLE", "DROP", "ELSE", "ELSEIF", "END", "ESCAPE", "EXCEPT", "EXCEPTION", "EXEC",
            "EXECUTE", "EXISTS", "EXIT", "EXTERNAL", "EXTRACT", "FALSE", "FETCH", "FIRST", "FLOAT", "FOR", "FOREIGN",
            "FOUND", "FROM", "FULL", "FUNCTION", "GET", "GLOBAL", "GO", "GOTO", "GRANT", "GROUP", "HANDLER",
            "HAVING", "HOUR", "IDENTITY", "IF", "IMMEDIATE", "IN", "INDICATOR", "INITIALLY", "INNER", "INOUT",
            "INPUT", "INSENSITIVE", "INSERT", "INT", "INTEGER", "INTERSECT", "INTERVAL", "INTO", "IS", "ISOLATION",
            "JOIN", "KEY", "LANGUAGE", "LAST", "LEADING", "LEAVE", "LEFT", "LEVEL", "LIKE", "LOCAL", "LOOP",
            "LOWER", "MATCH", "MAX", "MIN", "MINUTE", "MODULE", "MONTH", "NAMES", "NATIONAL", "NATURAL", "NCHAR",
            "NEXT", "NO", "NOT", "NULL", "NULLIF", "NUMERIC", "OCTET_LENGTH", "OF", "ON", "ONLY", "OPEN", "OPTION",
            "OR", "ORDER", "OUT", "OUTER", "OUTPUT", "OVERLAPS", "PAD", "PARAMETER", "PARTIAL", "PATH", "POSITION",
            "PRECISION", "PREPARE", "PRESERVE", "PRIMARY", "PRIOR", "PRIVILEGES", "PROCEDURE", "PUBLIC", "READ",
            "REAL", "REFERENCES", "RELATIVE", "REPEAT", "RESIGNAL", "RESTRICT", "RETURN", "RETURNS", "REVOKE",
            "RIGHT", "ROLLBACK", "ROUTINE", "ROWS", "SCHEMA", "SCROLL", "SECOND", "SECTION", "SELECT", "SESSION",
            "SESSION_USER", "SET", "SIGNAL", "SIZE", "SMALLINT", "SOME", "SPACE", "SPECIFIC", "SQL", "SQLCODE",
            "SQLERROR", "SQLEXCEPTION", "SQLSTATE", "SQLWARNING", "SUBSTRING", "SUM", "SYSTEM_USER", "TABLE",
            "TEMPORARY", "THEN", "TIME", "TIMESTAMP", "TIMEZONE_HOUR", "TIMEZONE_MINUTE", "TO", "TRAILING", "TRANSACTION",
            "TRANSLATE", "TRANSLATION", "TRIM", "TRUE", "UNDO", "UNION", "UNIQUE", "UNKNOWN", "UNTIL", "UPDATE",
            "UPPER", "USAGE", "USER", "USING", "VALUE", "VALUES", "VARCHAR", "VARYING", "VIEW", "WHEN", "WHENEVER",
            "WHERE", "WHILE", "WITH", "WORK", "WRITE", "YEAR", "ZONE");

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

    private final static Set<String> SQL_2003_KEYWORDS = strSet(
            "ADD", "ALL", "ALLOCATE", "ALTER", "AND", "ANY", "ARE", "ARRAY", "AS", "ASENSITIVE", "ASYMMETRIC",
            "AT", "ATOMIC", "AUTHORIZATION", "BEGIN", "BETWEEN", "BIGINT", "BINARY", "BLOB", "BOOLEAN", "BOTH",
            "BY", "CALL", "CALLED", "CASCADED", "CASE", "CAST", "CHAR", "CHARACTER", "CHECK", "CLOB", "CLOSE",
            "COLLATE", "COLUMN", "COMMIT", "CONDITION", "CONNECT", "CONSTRAINT", "CONTINUE", "CORRESPONDING",
            "CREATE", "CROSS", "CUBE", "CURRENT", "CURRENT_DATE", "CURRENT_DEFAULT_TRANSFORM_GROUP", "CURRENT_PATH",
            "CURRENT_ROLE", "CURRENT_TIME", "CURRENT_TIMESTAMP", "CURRENT_TRANSFORM_GROUP_FOR_TYPE", "CURRENT_USER",
            "CURSOR", "CYCLE", "DATE", "DAY", "DEALLOCATE", "DEC", "DECIMAL", "DECLARE", "DEFAULT", "DELETE",
            "DEREF", "DESCRIBE", "DETERMINISTIC", "DISCONNECT", "DISTINCT", "DO", "DOUBLE", "DROP", "DYNAMIC",
            "EACH", "ELEMENT", "ELSE", "ELSEIF", "END", "ESCAPE", "EXCEPT", "EXEC", "EXECUTE", "EXISTS", "EXIT",
            "EXTERNAL", "FALSE", "FETCH", "FILTER", "FLOAT", "FOR", "FOREIGN", "FREE", "FROM", "FULL", "FUNCTION",
            "GET", "GLOBAL", "GRANT", "GROUP", "GROUPING", "HANDLER", "HAVING", "HOLD", "HOUR", "IDENTITY", "IF",
            "IMMEDIATE", "IN", "INDICATOR", "INNER", "INOUT", "INPUT", "INSENSITIVE", "INSERT", "INT", "INTEGER",
            "INTERSECT", "INTERVAL", "INTO", "IS", "ITERATE", "JOIN", "LANGUAGE", "LARGE", "LATERAL", "LEADING",
            "LEAVE", "LEFT", "LIKE", "LOCAL", "LOCALTIME", "LOCALTIMESTAMP", "LOOP", "MATCH", "MEMBER", "MERGE",
            "METHOD", "MINUTE", "MODIFIES", "MODULE", "MONTH", "MULTISET", "NATIONAL", "NATURAL", "NCHAR", "NCLOB",
            "NEW", "NO", "NONE", "NOT", "NULL", "NUMERIC", "OF", "OLD", "ON", "ONLY", "OPEN", "OR", "ORDER",
            "OUT", "OUTER", "OUTPUT", "OVER", "OVERLAPS", "PARAMETER", "PARTITION", "PRECISION", "PREPARE", "PRIMARY",
            "PROCEDURE", "RANGE", "READS", "REAL", "RECURSIVE", "REF", "REFERENCES", "REFERENCING", "RELEASE",
            "REPEAT", "RESIGNAL", "RESULT", "RETURN", "RETURNS", "REVOKE", "RIGHT", "ROLLBACK", "ROLLUP", "ROW",
            "ROWS", "SAVEPOINT", "SCOPE", "SCROLL", "SEARCH", "SECOND", "SELECT", "SENSITIVE", "SESSION_USER",
            "SET", "SIGNAL", "SIMILAR", "SMALLINT", "SOME", "SPECIFIC", "SPECIFICTYPE", "SQL", "SQLEXCEPTION",
            "SQLSTATE", "SQLWARNING", "START", "STATIC", "SUBMULTISET", "SYMMETRIC", "SYSTEM", "SYSTEM_USER",
            "TABLE", "TABLESAMPLE", "THEN", "TIME", "TIMESTAMP", "TIMEZONE_HOUR", "TIMEZONE_MINUTE", "TO", "TRAILING",
            "TRANSLATION", "TREAT", "TRIGGER", "TRUE", "UNDO", "UNION", "UNIQUE", "UNKNOWN", "UNNEST", "UNTIL",
            "UPDATE", "USER", "USING", "VALUE", "VALUES", "VARCHAR", "VARYING", "WHEN", "WHENEVER", "WHERE",
            "WHILE", "WINDOW", "WITH", "WITHIN", "WITHOUT", "YEAR");

    private final static Set<String> MSSQL_KEYWORDS = strSet(
            "ADD", "ALL", "ALTER", "AND", "ANY", "AS", "ASC", "AUTHORIZATION", "BACKUP", "BEGIN", "BETWEEN",
            "BREAK", "BROWSE", "BULK", "BY", "CASCADE", "CASE", "CHECK", "CHECKPOINT", "CLOSE", "CLUSTERED",
            "COALESCE", "COLLATE", "COLUMN", "COMMIT", "COMPUTE", "CONSTRAINT", "CONTAINS", "CONTAINSTABLE",
            "CONTINUE", "CONVERT", "CREATE", "CROSS", "CURRENT", "CURRENT_DATE", "CURRENT_TIME", "CURRENT_TIMESTAMP",
            "CURRENT_USER", "CURSOR", "DATABASE", "DBCC", "DEALLOCATE", "DECLARE", "DEFAULT", "DELETE", "DENY",
            "DESC", "DISK", "DISTINCT", "DISTRIBUTED", "DOUBLE", "DROP", "DUMP", "ELSE", "END", "ERRLVL", "ESCAPE",
            "EXCEPT", "EXEC", "EXECUTE", "EXISTS", "EXIT", "EXTERNAL", "FETCH", "FILE", "FILLFACTOR", "FOR",
            "FOREIGN", "FREETEXT", "FREETEXTTABLE", "FROM", "FULL", "FUNCTION", "GOTO", "GRANT", "GROUP", "HAVING",
            "HOLDLOCK", "IDENTITY", "IDENTITYCOL", "IDENTITY_INSERT", "IF", "IN", "INDEX", "INNER", "INSERT",
            "INTERSECT", "INTO", "IS", "JOIN", "KEY", "KILL", "LEFT", "LIKE", "LINENO", "LOAD", "MERGE", "NATIONAL",
            "NOCHECK", "NONCLUSTERED", "NOT", "NULL", "NULLIF", "OF", "OFF", "OFFSETS", "ON", "OPEN", "OPENDATASOURCE",
            "OPENQUERY", "OPENROWSET", "OPENXML", "OPTION", "OR", "ORDER", "OUTER", "OVER", "PERCENT", "PIVOT",
            "PLAN", "PRECISION", "PRIMARY", "PRINT", "PROC", "PROCEDURE", "PUBLIC", "RAISERROR", "READ", "READTEXT",
            "RECONFIGURE", "REFERENCES", "REPLICATION", "RESTORE", "RESTRICT", "RETURN", "REVERT", "REVOKE",
            "RIGHT", "ROLLBACK", "ROWCOUNT", "ROWGUIDCOL", "RULE", "SAVE", "SCHEMA", "SECURITYAUDIT", "SELECT",
            "SEMANTICKEYPHRASETABLE", "SEMANTICSIMILARITYDETAILSTABLE", "SEMANTICSIMILARITYTABLE", "SESSION_USER",
            "SET", "SETUSER", "SHUTDOWN", "SOME", "STATISTICS", "SYSTEM_USER", "TABLE", "TABLESAMPLE", "TEXTSIZE",
            "THEN", "TO", "TOP", "TRAN", "TRANSACTION", "TRIGGER", "TRUNCATE", "TRY_CONVERT", "TSEQUAL", "UNION",
            "UNIQUE", "UNPIVOT", "UPDATE", "UPDATETEXT", "USE", "USER", "VALUES", "VARYING", "VIEW", "WAITFOR",
            "WHEN", "WHERE", "WHILE", "WITH", "WITHIN", "GROUP", "WRITETEXT");

    private final static Set<String> PGSQL_KEYWORDS = strSet(
            "ALL", "ANALYSE", "ANALYZE", "AND", "ANY", "ARRAY", "AS", "ASC", "ASYMMETRIC", "AUTHORIZATION", "BETWEEN",
            "BINARY", "BOTH", "CASE", "CAST", "CHECK", "COLLATE", "COLUMN", "CONSTRAINT", "CREATE", "CROSS",
            "CURRENT_DATE", "CURRENT_ROLE", "CURRENT_TIME", "CURRENT_TIMESTAMP", "CURRENT_USER", "DEFAULT", "DEFERRABLE",
            "DESC", "DISTINCT", "DO", "ELSE", "END", "EXCEPT", "FALSE", "FOR", "FOREIGN", "FREEZE", "FROM", "FULL",
            "GRANT", "GROUP", "HAVING", "ILIKE", "IN", "INITIALLY", "INNER", "INTERSECT", "INTO", "IS", "ISNULL",
            "JOIN", "LEADING", "LEFT", "LIKE", "LIMIT", "LOCALTIME", "LOCALTIMESTAMP", "NATURAL", "NEW", "NOT",
            "NOTNULL", "NULL", "OFF", "OFFSET", "OLD", "ON", "ONLY", "OR", "ORDER", "OUTER", "OVERLAPS", "PLACING",
            "PRIMARY", "REFERENCES", "RIGHT", "SELECT", "SESSION_USER", "SIMILAR", "SOME", "SYMMETRIC", "TABLE",
            "THEN", "TO", "TRAILING", "TRUE", "UNION", "UNIQUE", "USER", "USING", "VERBOSE", "WHEN", "WHERE");

    private final static Set<String> MYSQL_KEYWORDS = strSet(
            "ACCESSIBLE", "ADD", "ALL", "ALTER", "ANALYZE", "AND", "AS", "ASC", "ASENSITIVE", "BEFORE", "BETWEEN",
            "BIGINT", "BINARY", "BLOB", "BOTH", "BY", "CALL", "CASCADE", "CASE", "CHANGE", "CHAR", "CHARACTER",
            "CHECK", "COLLATE", "COLUMN", "CONDITION", "CONSTRAINT", "CONTINUE", "CONVERT", "CREATE", "CROSS",
            "CURRENT_DATE", "CURRENT_TIME", "CURRENT_TIMESTAMP", "CURRENT_USER", "CURSOR", "DATABASE", "DATABASES",
            "DAY_HOUR", "DAY_MICROSECOND", "DAY_MINUTE", "DAY_SECOND", "DEC", "DECIMAL", "DECLARE", "DEFAULT",
            "DELAYED", "DELETE", "DESC", "DESCRIBE", "DETERMINISTIC", "DISTINCT", "DISTINCTROW", "DIV", "DOUBLE",
            "DROP", "DUAL", "EACH", "ELSE", "ELSEIF", "ENCLOSED", "ESCAPED", "EXISTS", "EXIT", "EXPLAIN", "FALSE",
            "FETCH", "FLOAT", "FLOAT4", "FLOAT8", "FOR", "FORCE", "FOREIGN", "FROM", "FULLTEXT", "GRANT", "GROUP",
            "HAVING", "HIGH_PRIORITY", "HOUR_MICROSECOND", "HOUR_MINUTE", "HOUR_SECOND", "IF", "IGNORE", "IN",
            "INDEX", "INFILE", "INNER", "INOUT", "INSENSITIVE", "INSERT", "INT", "INT1", "INT2", "INT3", "INT4",
            "INT8", "INTEGER", "INTERVAL", "INTO", "IS", "ITERATE", "JOIN", "KEY", "KEYS", "KILL", "LEADING",
            "LEAVE", "LEFT", "LIKE", "LIMIT", "LINEAR", "LINES", "LOAD", "LOCALTIME", "LOCALTIMESTAMP", "LOCK",
            "LONG", "LONGBLOB", "LONGTEXT", "LOOP", "LOW_PRIORITY", "MASTER_SSL_VERIFY_SERVER_CERT", "MATCH",
            "MEDIUMBLOB", "MEDIUMINT", "MEDIUMTEXT", "MIDDLEINT", "MINUTE_MICROSECOND", "MINUTE_SECOND", "MOD",
            "MODIFIES", "NATURAL", "NOT", "NO_WRITE_TO_BINLOG", "NULL", "NUMERIC", "ON", "OPTIMIZE", "OPTION",
            "OPTIONALLY", "OR", "ORDER", "OUT", "OUTER", "OUTFILE", "PRECISION", "PRIMARY", "PROCEDURE", "PURGE",
            "RANGE", "READ", "READS", "READ_WRITE", "REAL", "REFERENCES", "REGEXP", "RELEASE", "RENAME", "REPEAT",
            "REPLACE", "REQUIRE", "RESTRICT", "RETURN", "REVOKE", "RIGHT", "RLIKE", "SCHEMA", "SCHEMAS", "SECOND_MICROSECOND",
            "SELECT", "SENSITIVE", "SEPARATOR", "SET", "SHOW", "SMALLINT", "SPATIAL", "SPECIFIC", "SQL", "SQLEXCEPTION",
            "SQLSTATE", "SQLWARNING", "SQL_BIG_RESULT", "SQL_CALC_FOUND_ROWS", "SQL_SMALL_RESULT", "SSL", "STARTING",
            "STRAIGHT_JOIN", "TABLE", "TERMINATED", "THEN", "TINYBLOB", "TINYINT", "TINYTEXT", "TO", "TRAILING",
            "TRIGGER", "TRUE", "UNDO", "UNION", "UNIQUE", "UNLOCK", "UNSIGNED", "UPDATE", "USAGE", "USE", "USING",
            "UTC_DATE", "UTC_TIME", "UTC_TIMESTAMP", "VALUES", "VARBINARY", "VARCHAR", "VARCHARACTER", "VARYING",
            "WHEN", "WHERE", "WHILE", "WITH", "WRITE", "XOR", "YEAR_MONTH", "ZEROFILL");

    private final static Set<String> DB2_KEYWORDS = strSet(
            "ABS", "ABSENT", "ABSOLUTE", "ACCESS", "ACCORDING", "ACTION", "ACTIVATE", "ADA", "ADD", "AFTER",
            "ALIAS", "ALL", "ALLOCATE", "ALLOW", "ALTER", "ALTERIN", "ALWAYS", "AND", "ANY", "APPEND", "ARE",
            "ARRAY", "AS", "ASC", "ASCII", "ASENSITIVE", "ASSERTION", "ASSOCIATE", "ASUTIME", "ASYMMETRIC", "AT",
            "ATOMIC", "ATTRIBUTES", "AUDIT", "AUTHORIZATION", "AUTOMATIC", "AUX", "AUXILIARY", "AVG", "BASE64",
            "BEFORE", "BEGIN", "BETWEEN", "BIGINT", "BINARY", "BIND", "BINDADD", "BIT", "BIT_LENGTH", "BLOB",
            "BLOCKED", "BOOLEAN", "BOTH", "BUFFERPOOL", "BY", "C", "C++", "CACHE", "CALL", "CALLED", "CAPTURE",
            "CARDINALITY", "CASCADE", "CASCADED", "CASE", "CAST", "CATALOG", "CCSID", "CEIL", "CEILING", "CHANGE",
            "CHAR", "CHARACTER", "CHARACTER_LENGTH", "CHAR_LENGTH", "CHECK", "CHECKED", "CLIENT", "CLOB", "CLOSE",
            "CLUSTER", "COALESCE", "COBOL", "COLLATE", "COLLATION", "COLLECT", "COLLECTION", "COLLID", "COLUMN",
            "COLUMNS", "COMMENT", "COMMIT", "COMMITTED", "COMPARISONS", "CONCAT", "CONDITION", "CONNECT", "CONNECTION",
            "CONSERVATIVE", "CONSTRAINT", "CONSTRAINTS", "CONTAINS", "CONTENT", "CONTINUE", "CONTROL", "CONVERT",
            "COPY", "CORR", "CORRELATION", "CORRESPONDING", "COUNT", "COUNT_BIG", "COVAR_POP", "COVAR_SAMP",
            "CREATE", "CREATEIN", "CREATETAB", "CROSS", "CUBE", "CUME_DIST", "CURRENT", "CURRENT_DATE", "CURRENT_DEFAULT_TRANSFORM_GROUP",
            "CURRENT_EXPLAIN_MODE", "CURRENT_LC_CTYPE", "CURRENT_PATH", "CURRENT_ROLE", "CURRENT_SCHEMA", "CURRENT_SERVER",
            "CURRENT_SQLID", "CURRENT_TIME", "CURRENT_TIMESTAMP", "CURRENT_TIMEZONE", "CURRENT_TRANSFORM_GROUP_FOR_TYPE",
            "CURRENT_USER", "CURSOR", "CYCLE", "DATA", "DATABASE", "DATALINK", "DATAPARTITIONNAME", "DATAPARTITIONNUM",
            "DATE", "DAY", "DAYS", "DB", "DB2DARI", "DB2GENERAL", "DB2GENRL", "DB2SQL", "DBADM", "DBCLOB", "DBINFO",
            "DBPARTITIONNAME", "DBPARTITIONNUM", "DEADLOCKS", "DEALLOCATE", "DEC", "DECIMAL", "DECLARE", "DEFAULT",
            "DEFAULTS", "DEFERRABLE", "DEFERRED", "DEFINE", "DEFINITION", "DEGREE", "DELETE", "DENSERANK", "DENSE_RANK",
            "DEREF", "DESC", "DESCRIBE", "DESCRIPTOR", "DETERMINISTIC", "DIAGNOSTICS", "DIMENSIONS", "DISABLE",
            "DISALLOW", "DISCONNECT", "DISTINCT", "DO", "DOCUMENT", "DOMAIN", "DOUBLE", "DROP", "DROPIN", "DSSIZE",
            "DYNAMIC", "EACH", "EBCDIC", "EDITPROC", "ELEMENT", "ELSE", "ELSEIF", "EMPTY", "ENABLE", "ENCODING",
            "ENCRYPTION", "END", "END-EXEC", "ENDING", "ERASE", "ESCAPE", "EUR", "EVENT", "EVERY", "EXACT", "EXCEPT",
            "EXCEPTION", "EXCLUDE", "EXCLUDING", "EXCLUSIVE", "EXEC", "EXECUTE", "EXISTS", "EXIT", "EXP", "EXPLAIN",
            "EXTENSION", "EXTERNAL", "EXTRACT", "FALSE", "FEDERATED", "FENCED", "FETCH", "FIELDPROC", "FILE",
            "FILTER", "FINAL", "FIRST", "FLOAT", "FLOOR", "FLUSH", "FOLLOWING", "FOR", "FORCE", "FOREIGN", "FORTRAN",
            "FOUND", "FREE", "FROM", "FS", "FULL", "FUNCTION", "FUSION", "G", "GENERAL", "GENERATED", "GET",
            "GLOBAL", "GO", "GOTO", "GRANT", "GRAPHIC", "GROUP", "GROUPING", "HANDLER", "HASH", "HASHED_VALUE",
            "HAVING", "HEX", "HINT", "HOLD", "HOUR", "HOURS", "IDENTITY", "IF", "IMMEDIATE", "IMPLICIT_SCHEMA",
            "IN", "INCLUDE", "INCLUDING", "INCLUSIVE", "INCREMENT", "INDEX", "INDICATOR", "INHERIT", "INITIALLY",
            "INITIAL_INSTS", "INITIAL_IOS", "INNER", "INOUT", "INPUT", "INSENSITIVE", "INSERT", "INSTEAD", "INSTS_PER_ARGBYTE",
            "INSTS_PER_INVOC", "INT", "INTEGER", "INTEGRITY", "INTERSECT", "INTERSECTION", "INTERVAL", "INTO",
            "IOS_PER_ARGBYTE", "IOS_PER_INVOC", "IS", "ISO", "ISOBID", "ISOLATION", "ITERATE", "JAR", "JAVA",
            "JIS", "JOIN", "K", "KEY", "LABEL", "LANGUAGE", "LARGE", "LAST", "LATERAL", "LC_CTYPE", "LEADING",
            "LEAVE", "LEFT", "LENGTH", "LEVEL", "LIKE", "LIMIT", "LINK", "LINKTYPE", "LN", "LOAD", "LOCAL", "LOCALDATE",
            "LOCALE", "LOCALTIME", "LOCALTIMESTAMP", "LOCATOR", "LOCATORS", "LOCK", "LOCKMAX", "LOCKS", "LOCKSIZE",
            "LONG", "LONGVAR", "LOOP", "LOWER", "M", "MAINTAINED", "MAPPING", "MATCH", "MATERIALIZED", "MAX",
            "MAXVALUE", "MEMBER", "MERGE", "METHOD", "MICROSECOND", "MICROSECONDS", "MIN", "MINUTE", "MINUTES",
            "MINVALUE", "MOD", "MODE", "MODIFIES", "MODULE", "MONITOR", "MONTH", "MONTHS", "MULTISET", "NAMED",
            "NAMES", "NAMESPACE", "NATIONAL", "NATURAL", "NCHAR", "NCLOB", "NEW", "NEW_TABLE", "NEXT", "NEXTVAL",
            "NICKNAME", "NIL", "NO", "NOCACHE", "NOCYCLE", "NODE", "NODENAME", "NODENUMBER", "NOMAXVALUE", "NOMINVALUE",
            "NONE", "NOORDER", "NORMALIZE", "NORMALIZED", "NOT", "NULL", "NULLABLE", "NULLIF", "NULLS", "NUMBER",
            "NUMERIC", "NUMPARTS", "OBID", "OBJECT", "OCTET_LENGTH", "OF", "OFF", "OLD", "OLD_TABLE", "OLE",
            "OLEDB", "ON", "ONCE", "ONLINE", "ONLY", "OPEN", "OPTIMIZATION", "OPTIMIZE", "OPTION", "OR", "ORDER",
            "OUT", "OUTER", "OUTPUT", "OVER", "OVERLAPS", "OVERLAY", "OVERRIDING", "PACKAGE", "PAD", "PADDED",
            "PAGESIZE", "PARALLEL", "PARAMETER", "PART", "PARTIAL", "PARTITION", "PARTITIONED", "PARTITIONING",
            "PARTITIONS", "PASCAL", "PASSING", "PASSTHRU", "PASSWORD", "PATH", "PCTFREE", "PERCENTILE_CONT",
            "PERCENTILE_DISC", "PERCENT_ARGBYTES", "PERCENT_RANK", "PERMISSION", "PIECESIZE", "PIPE", "PLAN",
            "PLI", "POSITION", "POWER", "PRECEDING", "PRECISION", "PREPARE", "PRESERVE", "PREVVAL", "PRIMARY",
            "PRIOR", "PRIQTY", "PRIVILEGES", "PROCEDURE", "PROGRAM", "PSID", "PUBLIC", "QUERY", "QUERYNO", "RANGE",
            "READ", "READS", "REAL", "RECOMMEND", "RECOVERY", "RECURSIVE", "REF", "REFERENCE", "REFERENCES",
            "REFERENCING", "REFRESH", "REGISTERS", "REGR_AVGX", "REGR_AVGY", "REGR_COUNT", "REGR_INTERCEPT",
            "REGR_R2", "REGR_SLOPE", "REGR_SXX", "REGR_SXY", "REGR_SYY", "RELATIVE", "RELEASE", "RENAME", "REPEAT",
            "REPEATABLE", "REPLACE", "REPLICATED", "RESET", "RESIGNAL", "RESOLVE", "RESTART", "RESTORE", "RESTRICT",
            "RESULT", "RESULT_SET_LOCATOR", "RETAIN", "RETURN", "RETURNING", "RETURNS", "RETURN_STATUS", "REVOKE",
            "RIGHT", "ROLLBACK", "ROLLUP", "ROUTINE", "ROW", "ROWID", "ROWNUMBER", "ROWS", "ROWSET", "ROW_COUNT",
            "RRN", "RUN", "S", "SAVEPOINT", "SBCS", "SCALE", "SCHEMA", "SCOPE", "SCRATCHPAD", "SCROLL", "SEARCH",
            "SECOND", "SECONDS", "SECQTY", "SECTION", "SECURITY", "SELECT", "SELECTIVITY", "SELF", "SENSITIVE",
            "SEQUENCE", "SERIALIZABLE", "SERVER", "SESSION", "SESSION_USER", "SET", "SETS", "SHARE", "SIGNAL",
            "SIMILAR", "SIMPLE", "SIZE", "SMALLINT", "SNAPSHOT", "SOME", "SOURCE", "SPACE", "SPECIFIC", "SPECIFICTYPE",
            "SQL", "SQLCODE", "SQLERROR", "SQLEXCEPTION", "SQLID", "SQLSTATE", "SQLWARNING", "SQRT", "STACKED",
            "STANDALONE", "STANDARD", "START", "STARTING", "STATE", "STATEMENT", "STATIC", "STATISTICS", "STAY",
            "STDDEV_POP", "STDDEV_SAMP", "STOGROUP", "STORAGE", "STORES", "STRIP", "STYLE", "SUBMULTISET", "SUBSTRING",
            "SUM", "SUMMARY", "SWITCH", "SYMMETRIC", "SYNONYM", "SYSFUN", "SYSIBM", "SYSPROC", "SYSTEM", "SYSTEM_USER",
            "TABLE", "TABLES", "TABLESPACE", "TABLESPACES", "TABLE_NAME", "TEMPORARY", "THEN", "THREADSAFE",
            "TIME", "TIMESTAMP", "TIMEZONE", "TIMEZONE_HOUR", "TIMEZONE_MINUTE", "TO", "TRAILING", "TRANSACTION",
            "TRANSFORM", "TRANSLATE", "TRANSLATION", "TREAT", "TRIGGER", "TRIM", "TRUE", "TYPE", "UESCAPE", "UNBOUNDED",
            "UNCOMMITTED", "UNDER", "UNDO", "UNICODE", "UNION", "UNIQUE", "UNKNOWN", "UNNEST", "UNTIL", "UNTYPED",
            "UPDATE", "UPPER", "URI", "URL", "USA", "USAGE", "USE", "USER", "USING", "VALID", "VALIDPROC", "VALUE",
            "VALUES", "VARCHAR", "VARGRAPHIC", "VARIABLE", "VARIANT", "VARYING", "VAR_POP", "VAR_SAMP", "VCAT",
            "VERSION", "VIEW", "VOLATILE", "VOLUMES", "WHEN", "WHENEVER", "WHERE", "WHILE", "WHITESPACE", "WIDTH_BUCKET",
            "WINDOW", "WITH", "WITHIN", "WITHOUT", "WLM", "WORK", "WRAPPER", "WRITE", "XML", "XMLAGG", "XMLATTRIBUTES",
            "XMLBINARY", "XMLCAST", "XMLCOMMENT", "XMLCONCAT", "XMLDECLARATION", "XMLDOCUMENT", "XMLELEMENT",
            "XMLELEMENT", "XMLEXISTS", "XMLFOREST", "XMLITERATE", "XMLNAMESPACES", "XMLPARSE", "XMLPI", "XMLQUERY",
            "XMLSCHEMA", "XMLSERIALIZE", "XMLTABLE", "XMLTEXT", "XMLVALIDATE", "XMLXSROBJECTID", "YEAR", "YEARS",
            "YES", "ZONE");

    private final static Set<String> ORACLE_KEYWORDS = strSet(
            "ACCESS", "ADD", "ALL", "ALTER", "AND", "ANY", "AS", "ASC", "AUDIT", "BETWEEN", "BY", "CHAR", "CHECK",
            "CLUSTER", "COLUMN", "COMMENT", "COMPRESS", "CONNECT", "CREATE", "CURRENT", "DATE", "DECIMAL", "DEFAULT",
            "DELETE", "DESC", "DISTINCT", "DROP", "ELSE", "EXCLUSIVE", "EXISTS", "FILE", "FLOAT", "FOR", "FROM",
            "GRANT", "GROUP", "HAVING", "IDENTIFIED", "IMMEDIATE", "IN", "INCREMENT", "INDEX", "INITIAL", "INSERT",
            "INTEGER", "INTERSECT", "INTO", "IS", "LEVEL", "LIKE", "LOCK", "LONG", "MAXEXTENTS", "MINUS", "MLSLABEL",
            "MODE", "MODIFY", "NOAUDIT", "NOCOMPRESS", "NOT", "NOWAIT", "NULL", "NUMBER", "OF", "OFFLINE", "ON",
            "ONLINE", "OPTION", "OR", "ORDER", "PCTFREE", "PRIOR", "PRIVILEGES", "PUBLIC", "RAW", "RENAME", "RESOURCE",
            "REVOKE", "ROW", "ROWID", "ROWNUM", "ROWS", "SELECT", "SESSION", "SET", "SHARE", "SIZE", "SMALLINT",
            "START", "SUCCESSFUL", "SYNONYM", "SYSDATE", "TABLE", "THEN", "TO", "TRIGGER", "UID", "UNION", "UNIQUE",
            "UPDATE", "USER", "VALIDATE", "VALUES", "VARCHAR", "VARCHAR2", "VIEW", "WHENEVER", "WHERE", "WITH");

    private final static Set<String> HQL_KEYWORDS = strSet(
            "ALL", "AND", "ANY", "AS", "ASCENDING", "AVG", "BETWEEN", "BOTH", "CASE", "CLASS", "COUNT", "DELETE",
            "DESCENDING", "DISTINCT", "ELEMENTS", "ELSE", "EMPTY", "END", "ESCAPE", "EXISTS", "FALSE", "FETCH",
            "FROM", "FULL", "GROUP", "HAVING", "IN", "INDICES", "INNER", "INSERT", "INTO", "IS", "JOIN", "LEADING",
            "LEFT", "LIKE", "MAX", "MEMBER", "MIN", "NEW", "NOT", "NULL", "OBJECT", "OF", "ON", "OR", "ORDER",
            "OUTER", "PROPERTIES", "RIGHT", "SELECT", "SET", "SOME", "SUM", "THEN", "TRAILING", "TRUE", "UNION",
            "UPDATE", "VERSIONED", "WHEN", "WHERE", "WITH");

    private final static Set<String> JPA_KEYWORDS = strSet(
            "ALL", "AND", "ANY", "AS", "ASC", "AVG", "BETWEEN", "BIT_LENGTH", "BY", "BY", "CHARACTER_LENGTH",
            "CHAR_LENGTH", "COUNT", "CURRENT_DATE", "CURRENT_TIME", "CURRENT_TIMESTAMP", "DELETE", "DESC", "DISTINCT",
            "EMPTY", "EXISTS", "FALSE", "FETCH", "FROM", "GROUP", "HAVING", "IN", "INNER", "IS", "JOIN", "LEFT",
            "LIKE", "LOWER", "MAX", "MEMBER", "MIN", "MOD", "NEW", "NOT", "NULL", "OBJECT", "OF", "OR", "ORDER",
            "OUTER", "POSITION", "SELECT", "SOME", "SUM", "TRIM", "TRUE", "UNKNOWN", "UPDATE", "UPPER", "WHERE");



    private final static byte[][][] lextabs = {
            LEX_SQL,     // SQL92
            LEX_SQL,     // SQL99
            LEX_SQL,     // SQL2003
            LEX_MSSQL,   // MSSQL
            LEX_SQL,     // PGSQL
            LEX_MYSQL,   // MYSQL
            LEX_SQL,     // DB2
            LEX_SQL,     // ORACLE
            LEX_SQL,     // HQL
            LEX_SQL      // JPA
    };



    public static final List<Set<String>> keywordSets = Collections.unmodifiableList(Arrays.asList(
            SQL_92_KEYWORDS,
            SQL_99_KEYWORDS,
            SQL_2003_KEYWORDS,
            MSSQL_KEYWORDS,
            PGSQL_KEYWORDS,
            MYSQL_KEYWORDS,
            DB2_KEYWORDS,
            ORACLE_KEYWORDS,
            HQL_KEYWORDS,
            JPA_KEYWORDS
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
