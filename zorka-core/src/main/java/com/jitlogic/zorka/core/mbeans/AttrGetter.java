/**
 * Copyright 2012-2015 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
 *
 * ZORKA is free software. You can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * ZORKA is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * ZORKA. If not, see <http://www.gnu.org/licenses/>.
 */

package com.jitlogic.zorka.core.mbeans;

import com.jitlogic.zorka.common.util.ObjectInspector;
import com.jitlogic.zorka.common.stats.ValGetter;

/**
 * Variant of AttrGetter that recursively fetches attribute from an object.
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class AttrGetter implements ValGetter {

    /**
     * This object attribute will be presented.
     */
    private Object obj;

    /**
     * Attribute chain
     */
    private Object[] attrs;


    /**
     * Creates value getter.
     *
     * @param obj   object
     * @param attrs attribute chain
     */
    public AttrGetter(Object obj, Object... attrs) {
        this.obj = obj;
        this.attrs = attrs;
    }


    @Override
    public Object get() {
        return ObjectInspector.get(obj, attrs);
    }

}
