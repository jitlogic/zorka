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
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this software. If not, see <http://www.gnu.org/licenses/>.
 */

package com.jitlogic.zorka.core.spy;

import java.util.Map;

/**
 * Spy Argument Processor interface. All argument processors must implement it.
 * Note that beanshell scripts can also make use of it with interface creation
 * feature. See documentation for more details.
 */
public interface SpyProcessor extends SpyDefArg {

    /**
     * Transforms record passed by instrumentation engine.
     *
     * @param record record to be processed
     *
     * @return processed record (can be the same as passed with record argument)
     *         or null to indicate that record should be dropped by instrumentation engine.
     */
    public Map<String,Object> process(Map<String,Object> record);
}
