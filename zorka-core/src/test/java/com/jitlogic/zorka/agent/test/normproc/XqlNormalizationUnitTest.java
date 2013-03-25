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
package com.jitlogic.zorka.agent.test.normproc;

import com.jitlogic.zorka.agent.normproc.GenericNormalizer;
import com.jitlogic.zorka.agent.normproc.NormLib;
import com.jitlogic.zorka.agent.normproc.Normalizer;

import static com.jitlogic.zorka.agent.normproc.NormLib.*;

import org.junit.Test;


import static org.junit.Assert.*;

public class XqlNormalizationUnitTest {

    Normalizer normalizer = GenericNormalizer.xql(DIALECT_SQL_99, NormLib.NORM_STD);

    @Test
    public void testNormalizeWhiteSpacesOnly() {
        assertEquals("select ff from mytab",
            normalizer.normalize(" SELECT ff\n FROM mytab\n"));
    }

    @Test
    public void testNormalizeSelectWithStar() {
        assertEquals("select * from mytab",
                normalizer.normalize(" SELECT\n * FROM mytab"));
    }
    // TODO normalize white spaces only

    // TODO normalize white spaces and symbols

    // TODO pass query with

}
