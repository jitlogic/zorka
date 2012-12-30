/**
 * Copyright 2012 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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
package com.jitlogic.zorka.spy.collectors;

import com.jitlogic.zorka.integ.snmp.SnmpLib;
import com.jitlogic.zorka.integ.snmp.SnmpTrapper;
import com.jitlogic.zorka.integ.snmp.TrapVarBindDef;
import com.jitlogic.contrib.libsnmp.*;
import com.jitlogic.zorka.spy.SpyProcessor;
import com.jitlogic.zorka.spy.SpyRecord;
import com.jitlogic.zorka.logproc.ZorkaLog;
import com.jitlogic.zorka.logproc.ZorkaLogger;
import com.jitlogic.zorka.util.ZorkaUtil;

public class SnmpCollector implements SpyProcessor {

    private ZorkaLog log = ZorkaLogger.getLog(this.getClass());

    private SnmpTrapper trapper;
    private SNMPObjectIdentifier oid;
    private int gtrap, strap;
    private String oprefix;

    private TrapVarBindDef[] varBindDefs;

    public SnmpCollector(SnmpTrapper trapper, String oid, int gtrap, int strap,
                         String oprefix, TrapVarBindDef...varBindDefs) {
        try {
            this.trapper = trapper;
            this.oprefix = oprefix;
            this.gtrap = gtrap; this.strap = strap;
            this.varBindDefs = ZorkaUtil.copyArray(varBindDefs);
            this.oid = new SNMPObjectIdentifier(oid);
        } catch (Exception e) {
            log.error("Error initializing SNMP-trap sending collector", e);
        }
    }



    public SpyRecord process(SpyRecord record) {
        SNMPVariablePair[] vars = new SNMPVariablePair[varBindDefs.length];

        try {
            for (int i = 0; i < vars.length; i++) {
                TrapVarBindDef vbd = varBindDefs[i];
                SNMPObjectIdentifier oid = new SNMPObjectIdentifier(oprefix + "." + vbd.getOidSuffix());
                SNMPObject val = SnmpLib.val(vbd.getType(), record.get(vbd.getSlot()));
                val = val != null ? val : new SNMPNull();
                vars[i] = new SNMPVariablePair(oid, val);
            }
            trapper.trap(gtrap, strap, oid, vars);
        } catch (SNMPBadValueException e) {
            log.error("Error submitting record to SNMP trapper", e);
        }

        return record;
    }

}
