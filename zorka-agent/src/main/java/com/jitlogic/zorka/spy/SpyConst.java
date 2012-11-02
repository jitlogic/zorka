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


    // Debug levels

    /** Be quiet */
    public final static int SPD_NONE = 0;

    /** Basic status messages */
    public final static int SPD_STATUS = 1;

    /** Detailed configuration information */
    public final static int SPD_CONFIG = 2;

    /** Log transformed classes */
    public final static int SPD_CLASSXFORM = 3;

    /** Log transformed methods */
    public final static int SPD_METHODXFORM = 4;

    /** Log all collected records reaching collector dispatcher */
    public final static int SPD_CDISPATCHES = 5;

    /** Log all collected records on each collector */
    public final static int SPD_COLLECTORS = 6;

    /** Log all argument processing events */
    public final static int SPD_ARGPROC = 7;

    /** Log all submissions from instrumented code */
    public final static int SPD_SUBMISSIONS = 8;

    /** Log all encountered methods (only from transformed classes) */
    public final static int SPD_METHODALL = 9;

    /** Log all classes going through transformer */
    public final static int SPD_CLASSALL = 10;

    /** Maximum possible debug log level */
    public final static int SPD_MAX = 10;

}
