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
package com.jitlogic.zorka.xxlproc;

import static com.jitlogic.zorka.xxlproc.XqlToken.*;

public class XxLib {

    public final static int NORM_STD = (1<<UNKNOWN)|(1<<WHITESPACE)|(1<<SYMBOL)|(1<<LITERAL)|(1<<COMMENT)|(1<<KEYWORD);

    /**
     * Generic SQL dialect (based on SQL99)
     */
    public final static int DIALECT_SQL99 = 0;

    public Normalizer sqlNormalizer(int dialect) {
        return new XqlNormalizer(dialect, NORM_STD);
    }
}
