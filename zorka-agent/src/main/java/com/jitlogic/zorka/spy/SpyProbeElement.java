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

import static com.jitlogic.zorka.spy.SpyLib.*;

/**
 *
 */
public class SpyProbeElement {

    private int argType;
    private String className;

    public SpyProbeElement(Object arg) {
        if (arg instanceof String) {
            className = (String)arg;
            argType = FETCH_CLASS;
        } else if (arg instanceof Integer) {
            argType = (Integer)arg;
        }

    }

    public int getArgType() {
        return argType;
    }

    public String getClassName() {
        return className;
    }

}
