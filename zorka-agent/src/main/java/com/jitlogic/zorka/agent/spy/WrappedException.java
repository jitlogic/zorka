/**
 * Copyright 2012-2013 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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

package com.jitlogic.zorka.agent.spy;

import com.jitlogic.zorka.common.TracedException;

/**
 * Represents wrapped exceptions in a form that is compatible with symbolic
 * exceptions.
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class WrappedException implements TracedException {

    private Throwable exception;

    /**
     * Creates new wrapper around exception.
     *
     * @param exception exception to be wrapper.
     */
    public WrappedException(Throwable exception) {
        this.exception = exception;
    }


    public Throwable getException() {
        return exception;
    }

    @Override
    public String toString() {
        return exception.toString();
    }

    @Override
    public int hashCode() {
        return exception.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof WrappedException &&
            exception.equals(((WrappedException)obj).exception);
    }
}
