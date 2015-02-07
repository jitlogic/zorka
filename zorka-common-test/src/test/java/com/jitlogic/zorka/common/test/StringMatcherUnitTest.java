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
package com.jitlogic.zorka.common.test;


import com.jitlogic.zorka.common.util.StringMatcher;

import org.junit.Test;

import static org.junit.Assert.*;

import java.util.Arrays;

public class StringMatcherUnitTest {

    @Test
    public void testMatchCasesWithExcludeRules() throws Exception {
        StringMatcher m = new StringMatcher(null,
                Arrays.asList("aaa", "bbb", "~cc.*cc", "~ddd"));

        assertFalse(m.matches("aaa"));
        assertTrue(m.matches("aa"));
        assertFalse(m.matches("bbb"));
        assertTrue(m.matches("ccaac"));
        assertFalse(m.matches("ccaacc"));
        assertFalse(m.matches("ddd"));
    }


    @Test
    public void testMatchWithIncludeRules() throws Exception {
        StringMatcher m = new StringMatcher(
                Arrays.asList("~a.*"),
                Arrays.asList("aaa", "bbb"));

        assertTrue(m.matches("aa"));
        assertFalse(m.matches("aaa"));
        assertFalse(m.matches("bbb"));
        assertFalse(m.matches("ccc"));
    }

}
