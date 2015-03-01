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

package com.jitlogic.zorka.core.test.spy.support;

import com.jitlogic.contrib.libsnmp.SNMPObjectIdentifier;
import com.jitlogic.contrib.libsnmp.SNMPVariablePair;
import com.jitlogic.zorka.core.integ.SnmpLib;
import com.jitlogic.zorka.core.integ.SnmpTrapper;

import java.util.ArrayList;
import java.util.List;

public class TestSnmpTrapper extends SnmpTrapper {

    public static class SnmpRec {
        public final int gtrap;
        public final int strap;
        public final SNMPObjectIdentifier oid;
        public final SNMPVariablePair[] vars;
        public SnmpRec(int gtrap, int strap, SNMPObjectIdentifier oid, SNMPVariablePair[] vars) {
            this.gtrap = gtrap;
            this.strap = strap;
            this.oid = oid;
            this.vars = vars;
        }
    }

    private List<SnmpRec> recs = new ArrayList<SnmpRec>();

    public TestSnmpTrapper() {
        super("127.0.0.1", "public", "127.0.0.1", SnmpLib.SNMP_V1);
    }

    public void trap(int gtrap, int strap, SNMPObjectIdentifier oid, SNMPVariablePair...vars) {
        recs.add(new SnmpRec(gtrap, strap, oid, vars));
    }

    public int size() {
        return recs.size();
    }

    public SnmpRec get(int idx) {
        return recs.get(idx);
    }
}
