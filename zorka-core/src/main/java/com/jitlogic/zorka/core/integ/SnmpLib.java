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

package com.jitlogic.zorka.core.integ;

import com.jitlogic.contrib.libsnmp.*;
import com.jitlogic.zorka.common.ZorkaService;
import com.jitlogic.zorka.common.util.ZorkaConfig;
import com.jitlogic.zorka.common.util.ObjectInspector;
import com.jitlogic.zorka.common.util.ZorkaLog;
import com.jitlogic.zorka.common.util.ZorkaLogger;
import com.jitlogic.zorka.common.util.ZorkaUtil;

import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SNMP library manages SNMP trappers and contains set of functions for manipulating SNMP data types
 * and set of SNMP-specific constants.
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class SnmpLib implements ZorkaService {

    /**
     * Logger.
     */
    private static final ZorkaLog log = ZorkaLogger.getLog(SnmpLib.class);

    /**
     * SNMPv1 protocol version
     */
    public static final int SNMP_V1 = 1;

    /**
     * SNMPv2 protocol version
     */
    public static final int SNMP_V2 = 2;

    /**
     * Standard OID prefix for MGMT MIB
     */
    public static final String MGMT_MIB = "1.3.6.1.2.1";

    /**
     * SNMP Integer type
     */
    public static final int INTEGER = 0;

    /**
     * SNMP Bit String type
     */
    public static final int BITSTRING = 1;

    /**
     * SNMP Octet String type
     */
    public static final int OCTETSTRING = 2;

    /**
     * Null type
     */
    public static final int NULL = 3;

    /**
     * OID type
     */
    public static final int OID = 4;

    /**
     * Sequence type
     */
    public static final int SEQUENCE = 5;

    /**
     * IP address type
     */
    public static final int IPADDRESS = 6;

    /**
     * 32-bit counter type
     */
    public static final int COUNTER32 = 7;

    /**
     * 32-bit gauge type
     */
    public static final int GAUGE32 = 8;

    /**
     * Timestamp type
     */
    public static final int TIMETICKS = 9;

    /**
     * Opaque type (binary blob to be parsed by application-specific code
     */
    public static final int OPAQUE = 10;

    /**
     * NSAP address type
     */
    public static final int NSAPADDRESS = 11;

    /**
     * 64-bit counter type
     */
    public static final int COUNTER64 = 12;

    /**
     * 32-bit unsigned integer type
     */
    public static final int UINTEGER32 = 13;

    /**
     * COLDSTART trap type represents device cold start events
     */
    public static final int GT_COLDSTART = 0;

    /**
     * WARMSTART trap type represents device warm start events
     */
    public static final int GT_WARMSTART = 1;

    /**
     * LINKDOWN trap type represents connectivity loss events
     */
    public static final int GT_LINKDOWN = 2;

    /**
     * LINKUP trap type represents connectivity restore events
     */
    public static final int GT_LINKUP = 3;

    /**
     * AUTHFAIL trap type represents authentication failures
     */
    public static final int GT_AUTHFAIL = 4;

    /**
     * EGP loss trap type
     */
    public static final int GT_EGPLOSS = 5;

    /**
     * Application-specific trap types (propably most useful for java applications)
     */
    public static final int GT_SPECIFIC = 6;

    /**
     * SNMP trappers registered
     */
    private Map<String, SnmpTrapper> trappers = new ConcurrentHashMap<String, SnmpTrapper>();

    private ZorkaConfig config;

    public SnmpLib(ZorkaConfig config) {
        this.config = config;
    }

    /**
     * Creates SNMP object identifier using template and supplied values.
     *
     * @param template OID template
     * @param vals     values to be
     * @return
     * @throws SNMPBadValueException
     */
    public SNMPObjectIdentifier oid(String template, Object... vals) throws SNMPBadValueException {
        return new SNMPObjectIdentifier(template.contains("$") ? ObjectInspector.substitute(template, vals) : template);
    }


    /**
     * Creates SNMP value object
     *
     * @param type object type
     * @param val  object value
     * @return SNMP value
     */
    public static SNMPObject val(int type, Object val) {
        try {
            switch (type) {
                case INTEGER:
                    return new SNMPInteger((Long) ZorkaUtil.coerce(val, Long.class));
                case BITSTRING:
                    if (val instanceof byte[]) {
                        return new SNMPBitString((byte[]) val);
                    } else {
                        return new SNMPBitString("" + val);
                    }
                case OCTETSTRING:
                    if (val instanceof byte[]) {
                        return new SNMPOctetString((byte[]) val);
                    } else {
                        return new SNMPOctetString("" + val);
                    }
                case NULL:
                    return new SNMPNull();
                case OID:
                    return new SNMPObjectIdentifier("" + val);
                case SEQUENCE:
                    return new SNMPSequence((Vector) val);
                case IPADDRESS:
                    if (val instanceof byte[]) {
                        return new SNMPIPAddress((byte[]) val);
                    } else {
                        return new SNMPIPAddress("" + val);
                    }
                case COUNTER32:
                    return new SNMPCounter32((Long) ZorkaUtil.coerce(val, Long.class));
                case COUNTER64:
                    return new SNMPCounter64((Long) ZorkaUtil.coerce(val, Long.class));
                case GAUGE32:
                    return new SNMPGauge32((Long) ZorkaUtil.coerce(val, Long.class));
                case TIMETICKS:
                    return new SNMPTimeTicks((Long) ZorkaUtil.coerce(val, Long.class));
                case OPAQUE:
                    return null;
                case NSAPADDRESS:
                    if (val instanceof byte[]) {
                        return new SNMPNSAPAddress((byte[]) val);
                    } else {
                        return new SNMPNSAPAddress((String) val);
                    }
                case UINTEGER32:
                    return new SNMPUInteger32((Long) ZorkaUtil.coerce(val, Long.class));
                default:
                    log.error(ZorkaLogger.ZAG_ERRORS, "Invalid type code passed to val(): " + type);
                    break;
            }
        } catch (SNMPBadValueException e) {
            log.error(ZorkaLogger.ZAG_ERRORS, "Error creating value", e);
        }
        return null;
    }


    /**
     * Looks for registered SNMP trapper
     *
     * @param id trapper identifier
     * @return SNMP trapper or null
     */
    public SnmpTrapper trapper(String id) {
        return trappers.get(id);
    }


    /**
     * Looks for registered SNMP trapper. Starts and registers SNMPv1 trapper if none has been registered yet.
     *
     * @param id        trapper ID
     * @param addr      IP address of device receiving traps
     * @param community community ID
     * @param agentAddr SNMP agent address (added to SNMP trap packet)
     * @return SNMP trapper
     */
    public SnmpTrapper trapper(String id, String addr, String community, String agentAddr) {
        return trapper(id, addr, community, agentAddr, SNMP_V1);
    }


    /**
     * Looks for registered SNMP trapper. Starts and registers SNMPv1 trapper if none has been registered yet.
     *
     * @param id        trapper ID
     * @param addr      IP address of device receiving traps
     * @param community community ID
     * @param agentAddr SNMP agent address (added to SNMP trap packet)
     * @param protocol  protocol version (SNMPv1 or SNMPv2)
     * @return SNMP trapper
     */
    public SnmpTrapper trapper(String id, String addr, String community, String agentAddr, int protocol) {
        SnmpTrapper trapper = trappers.get(id);

        if (trapper == null) {
            trapper = new SnmpTrapper(config.formatCfg(addr), config.formatCfg(community),
                    config.formatCfg(agentAddr), protocol);
            trappers.put(id, trapper);
            trapper.start();
        }

        return trapper;
    }


    /**
     * Stops and removes SNMP trapper.
     *
     * @param id trapper ID
     */
    public void remove(String id) {
        SnmpTrapper trapper = trappers.remove(id);

        if (trapper != null) {
            trapper.close();
            trapper.stop();
        }
    }

    @Override
    public void shutdown() {
        for (SnmpTrapper trapper : trappers.values()) {
            trapper.close();
            trapper.stop();
        }
        trappers.clear();
    }


    /**
     * Creates new spy record field to SNMP trap attribute binding.
     *
     * @param field     field name in spy record
     * @param type      SNMP data type
     * @param oidSuffix suffix added to attribute OID
     * @return binding object
     */
    public TrapVarBindDef bind(String field, int type, String oidSuffix) {
        return new TrapVarBindDef(field, type, oidSuffix);
    }
}
