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

package com.jitlogic.zorka.common.stats;

/**
 * Value getter interface. Objects implementing this interface can be registered as
 * attributes to ZorkaMappedBean objects. When client request attribute value, mbean
 * will call get() method and return result to client.
 */
public interface ValGetter {

    /**
     * This method is called when JMX client fetches attribute value.
     *
     * @return
     */
    public Object get();

}
