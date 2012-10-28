/**
 * Copyright 2012 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
 * <p/>
 * This is free software. You can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * <p/>
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 * <p/>
 * You should have received a copy of the GNU Lesser General Public License
 * along with this software. If not, see <http://www.gnu.org/licenses/>.
 */

package com.jitlogic.zorka.spy.collectors;

import com.jitlogic.zorka.vmsci.SpyCollector;

public class JmxAttrCollector implements SpyCollector {

    public JmxAttrCollector(String mbsName, String beanName, String attrName) {
        this(mbsName, beanName, attrName, null);
    }

    public JmxAttrCollector(String mbsName, String beanName, String attrName, String statAttr) {

    }

    public void collect(int type, int id, boolean submit, Object[] vals) {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
