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
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this software. If not, see <http://www.gnu.org/licenses/>.
 */

package com.jitlogic.zorka.spy;

public interface SpyConst {

    public static final int ON_ENTER   = 0;
    public static final int ON_EXIT    = 1;
    public static final int ON_ERROR   = 2;
    public static final int ON_SUBMIT  = 3;
    public static final int ON_COLLECT = 4;


    public static final String SM_NOARGS      = "<no-args>";
    public static final String SM_CONSTRUCTOR = "<init>";
    public static final String SM_ANY_TYPE    = null;
    public static final String SM_STATIC      = "<clinit>";


    public final static int SF_NONE = 0;
    public final static int SF_IMMEDIATE = 1;
    public final static int SF_FLUSH = 2;

}
