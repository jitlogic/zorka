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
package com.jitlogic.zorka.central.test;


import com.jitlogic.zorka.central.db.DbRecord;
import com.jitlogic.zorka.central.db.DbUtil;
import com.jitlogic.zorka.central.test.support.CentralFixture;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class DatabaseUnitTest extends CentralFixture {

    @Test
    public void testIfTablesHaveBeenCreated() throws Exception {
        DbUtil util = instance.getDbUtil();

        List<DbRecord> rec = util.select("select count(1) as cnt from SYMBOLS");
        assertEquals(1, rec.size());
    }

}
