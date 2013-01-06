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

import bsh.EvalError;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Main submitter contains static methods that can be called directly by
 * instrumentation probes. It forwards requests to actual submitter that
 * can be configured using setSubmitter() method.
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class MainSubmitter {

    /** Submitter receiving actual submissions */
    private static SpySubmitter submitter;

    /** Error counter */
    private static AtomicLong errorCount = new AtomicLong(0);

    /**
     * This method is called by spy probes.
     *
     * @param stage entry, return point or error handling point of spy probe
     *
     * @param id spy context ID
     *
     * @param submitFlags submit flags
     *
     * @param vals values fetched by probe
     */
    public static void submit(int stage, int id, int submitFlags, Object[] vals) {
        try {
            if (submitter != null) {
                submitter.submit(stage, id, submitFlags, vals);
            }
        } catch (EvalError e) {
            errorCount.incrementAndGet();
        } catch (Exception e) {
            errorCount.incrementAndGet();
        }
    }

    /**
     * Sets backing spy submitter
     *
     * @param submitter spy submitter
     */
    public static void setSubmitter(SpySubmitter submitter) {
        MainSubmitter.submitter = submitter;
    }


    /**
     * Returns error count.
     *
     * @return error count
     */
    public static long getErrorCount() {
        return errorCount.longValue();
    }

}
