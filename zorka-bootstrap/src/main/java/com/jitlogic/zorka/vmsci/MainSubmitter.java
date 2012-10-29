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

package com.jitlogic.zorka.vmsci;

public class MainSubmitter {

    public final static int SF_IMMEDIATE = 1;
    public final static int SF_FLUSH = 2;

    private static SpySubmitter submitter = null;
    private static long errorCount = 0;

    public static void submit(int stage, int id, int submitFlags, Object[] vals) {
        try {
            if (submitter != null) {
                submitter.submit(stage, id, submitFlags, vals);
            }
        } catch (Throwable e) {
            synchronized (MainSubmitter.class) {
                errorCount++;
            }
        }
    }

    public static void setSubmitter(SpySubmitter submitter) {
        MainSubmitter.submitter = submitter;
    }

    public static long getErrorCount() {
        return errorCount;
    }

}
