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
package com.jitlogic.zorka.core.spy.plugins;

import com.jitlogic.zorka.core.integ.SnmpLib;
import com.jitlogic.zorka.core.integ.SnmpTrapper;
import com.jitlogic.zorka.core.integ.TrapVarBindDef;
import com.jitlogic.contrib.libsnmp.*;
import com.jitlogic.zorka.common.util.ZorkaLog;
import com.jitlogic.zorka.common.util.ZorkaLogger;
import com.jitlogic.zorka.common.util.ZorkaUtil;
import com.jitlogic.zorka.core.spy.SpyProcessor;

import java.util.Map;

/**
 * This collector sends SNMP trap for each processed record. As SNMP traps can be structured,
 * this cannot be done using ordinary trapper collector.
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class SnmpCollector implements SpyProcessor {

    /**
     * Logger object
     */
    private ZorkaLog log = ZorkaLogger.getLog(this.getClass());

    /**
     * SNMP trapper
     */
    private SnmpTrapper trapper;

    /**
     * Enterprise OID
     */
    private SNMPObjectIdentifier oid;

    /**
     * Trap type group
     */
    private int gtrap;

    /**
     * Trap type
     */
    private int strap;

    /**
     * OID prefix for trap attributes
     */
    private String oprefix;

    /**
     * Record fields - trap attributes map.
     */
    private TrapVarBindDef[] varBindDefs;

    /**
     * Creates SNMP collector.
     *
     * @param trapper     SNMP trapper
     * @param oid         enterprise OID
     * @param gtrap       trap type group
     * @param strap       trap type
     * @param oprefix     OID prefix for attributes
     * @param varBindDefs mappings from record fields to trap attributes
     */
    public SnmpCollector(SnmpTrapper trapper, String oid, int gtrap, int strap,
                         String oprefix, TrapVarBindDef... varBindDefs) {
        try {
            this.trapper = trapper;
            this.oprefix = oprefix;
            this.gtrap = gtrap;
            this.strap = strap;
            this.varBindDefs = ZorkaUtil.copyArray(varBindDefs);
            this.oid = new SNMPObjectIdentifier(oid);
        } catch (Exception e) {
            log.error(ZorkaLogger.ZAG_ERRORS, "Error initializing SNMP-trap sending collector", e);
        }
    }


    @Override
    public Map<String, Object> process(Map<String, Object> record) {
        SNMPVariablePair[] vars = new SNMPVariablePair[varBindDefs.length];

        try {
            for (int i = 0; i < vars.length; i++) {
                TrapVarBindDef vbd = varBindDefs[i];
                SNMPObjectIdentifier oid = new SNMPObjectIdentifier(oprefix + "." + vbd.getOidSuffix());
                SNMPObject val = SnmpLib.val(vbd.getSnmpDataType(), record.get(vbd.getSourceField()));
                val = val != null ? val : new SNMPNull();
                vars[i] = new SNMPVariablePair(oid, val);
            }
            trapper.trap(gtrap, strap, oid, vars);
        } catch (SNMPBadValueException e) {
            log.error(ZorkaLogger.ZAG_ERRORS, "Error submitting record to SNMP trapper", e);
        }

        return record;
    }

}
