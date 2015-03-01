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

package com.jitlogic.zorka.common.test.support;

import java.util.Properties;

/**
 * @author RLE <rafal.lewczuk@gmail.com>
 */
public class TestInspectorClass {

    private static int priv = 234;

    public static int count = 123;

    private Properties props;

    public TestInspectorClass() {
    }

    public TestInspectorClass(Properties props) {
        this.props = props;
    }

    public static int count() {
        return count + 3;
    }

    public static int getCount() {
        return count + 2;
    }

    protected Properties getProperties() {
        return props;
    }

}
