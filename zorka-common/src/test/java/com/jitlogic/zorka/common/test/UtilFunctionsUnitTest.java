/*
 * Copyright 2012-2020 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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

package com.jitlogic.zorka.common.test;

import com.jitlogic.zorka.common.util.ZorkaUtil;
import org.junit.Test;

import static org.junit.Assert.*;

import java.io.File;

public class UtilFunctionsUnitTest {

    private String _u(String path) {
        return path.replace("/", File.separator);
    }

    @Test
    public void testUtilPathFn() {
        assertEquals(_u("/tmp/asd"), _u(ZorkaUtil.path(_u("/tmp"), "asd")));
        assertEquals(_u("/tmp/asd"), _u(ZorkaUtil.path(_u("/tmp/"), "/asd/")));
    }


    @Test
    public void testUtilClipArray() {
        assertArrayEquals(ZorkaUtil.clipArray(new String[]{"a", "b"}, -1), new String[]{"a"});
    }

}
