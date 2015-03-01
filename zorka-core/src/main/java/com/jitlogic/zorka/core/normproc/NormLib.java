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
package com.jitlogic.zorka.core.normproc;

import static com.jitlogic.zorka.core.normproc.XqlLexer.*;

/**
 * Library with functions for handling normalizers.
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class NormLib {

    /**
     * Normalize whitespaces, symbols and keywords. Remove comments and unknown tokens, leave literals.
     */
    public final static int NORM_MIN =
            (1<<T_UNKNOWN)|(1<<T_WHITESPACE)|(1<<T_SYMBOL)|(1<<T_COMMENT)|(1<<T_KEYWORD);

    /**
     * Normalize whitespaces, symbols and keywords. Remove comments and unknown tokens, replace literals with placeholders.
     */
    public final static int NORM_STD =
            (1<<T_UNKNOWN)|(1<<T_WHITESPACE)|(1<<T_SYMBOL)|(1<<T_LITERAL)|(1<<T_COMMENT)|(1<<T_KEYWORD);

    /** ANSI SQL-92 standard */
    public static final int DIALECT_SQL_92 = 0;

    /** ANSI SQL-99 standard */
    public static final int DIALECT_SQL_99 = 1;

    /** ANSI SQL-2003 standard */
    public static final int DIALECT_SQL_03 = 2;

    /** Microsoft T-SQL */
    public static final int DIALECT_MSSQL  = 3;

    /** Postgres PSQL */
    public static final int DIALECT_PGSQL  = 4;

    /** MySQL dialect */
    public static final int DIALECT_MYSQL  = 5;

    /** IBM DB2 SQL dialect */
    public static final int DIALECT_DB2    = 6;

    /** Oracle PL-SQL */
    public static final int DIALECT_ORACLE = 7;

    /** Hibernate HQL */
    public static final int DIALECT_HQL    = 8;

    /** Query language for JPA */
    public static final int DIALECT_JPA    = 9;

    /**
     * Creates SQL/*QL normalizer.
     *
     * @param dialect SQL dialect (see DIALECT_* constants)
     *
     * @param flags normalization flags (see NORM_* constants)
     *
     * @return normalizer object
     */
    public Normalizer sql(int dialect, int flags) {
        return GenericNormalizer.xql(dialect, flags);
    }

    /**
     * Creates normalizer for LDAP queries.
     *
     * @param flags normalization flags (see NORM_* constants)
     *
     * @return normalizer object
     */
    public Normalizer ldap(int flags) {
        return GenericNormalizer.ldap(flags);
    }
}
