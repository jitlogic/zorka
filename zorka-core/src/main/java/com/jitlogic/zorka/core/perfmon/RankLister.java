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
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this software. If not, see <http://www.gnu.org/licenses/>.
 */
package com.jitlogic.zorka.core.perfmon;

import java.util.List;

/**
 * Rank listers are used to scan for monitored objects and wrap them
 * into Rankable wrappers, so they can be used to construct rank lists.
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public interface RankLister<T extends Rankable<?>> {

    /**
     * Returns a list of wrapped items to be used to construct a ranking.
     *
     * Returned list must be mutable and rank lister object loses control
     * over it (that means, any object receiving this list can modify it
     * without consequences).
     *
     * @return list of items to construct ranking
     */
    public List<T> list();

}
