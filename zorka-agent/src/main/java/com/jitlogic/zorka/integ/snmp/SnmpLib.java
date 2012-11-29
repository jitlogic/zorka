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

package com.jitlogic.zorka.integ.snmp;

import com.jitlogic.zorka.libsnmp.*;
import com.jitlogic.zorka.util.ObjectInspector;
import com.jitlogic.zorka.util.ZorkaLog;
import com.jitlogic.zorka.util.ZorkaLogger;
import com.jitlogic.zorka.util.ZorkaUtil;

import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

public class SnmpLib {

    private ZorkaLog log = ZorkaLogger.getLog(this.getClass());

    // Protocol versions
    public final static int SNMP_V1 = 1;
    public final static int SNMP_V2 = 2;

    // Basic types
    public final static String MIB_ISO      = "1";
    public final static String MIB_INTERNET = "1.3.6.1";

    // Basic types
    public static final int INTEGER     = 0;
    public static final int BITSTRING   = 1;
    public static final int OCTETSTRING = 2;
    public static final int NULL        = 3;
    public static final int OID         = 4;
    public static final int SEQUENCE    = 5;
    public static final int IPADDRESS   = 6;
    public static final int COUNTER32   = 7;
    public static final int GAUGE32     = 8;
    public static final int TIMETICKS   = 9;
    public static final int OPAQUE      = 10;
    public static final int NSAPADDRESS = 11;
    public static final int COUNTER64   = 12;
    public static final int UINTEGER32  = 13;


    private ObjectInspector inspector = new ObjectInspector();

    private Map<String,SnmpTrapper> trappers = new ConcurrentHashMap<String, SnmpTrapper>();

    public SNMPObjectIdentifier oid(String template, Object...vals) throws SNMPBadValueException {
        return new SNMPObjectIdentifier(template.contains("$") ? inspector.substitute(template, vals) : template);
    }


    public static SNMPObject val(int type, Object val) {
        try {
            switch (type) {
                case INTEGER :
                    return new SNMPInteger((Long)ZorkaUtil.coerce(val, Long.class));
                case BITSTRING :
                    if (val instanceof byte[]) {
                        return new SNMPBitString((byte[])val);
                    } else {
                        return new SNMPBitString((String)val);
                    }
                case OCTETSTRING :
                    if (val instanceof byte[]) {
                        return new SNMPOctetString((byte[])val);
                    } else {
                        return new SNMPOctetString((String)val);
                    }
                case NULL :
                    return new SNMPNull();
                case OID:
                    return new SNMPObjectIdentifier((String)val);
                case SEQUENCE:
                    return new SNMPSequence((Vector)val);
                case IPADDRESS:
                    if (val instanceof byte[]) {
                        return new SNMPIPAddress((byte[])val);
                    } else {
                        return new SNMPIPAddress((String)val);
                    }
                case COUNTER32:
                    return new SNMPCounter32((Long)ZorkaUtil.coerce(val, Long.class));
                case COUNTER64:
                    return new SNMPCounter64((Long)ZorkaUtil.coerce(val, Long.class));
                case GAUGE32:
                    return new SNMPGauge32((Long)ZorkaUtil.coerce(val, Long.class));
                case TIMETICKS:
                    return new SNMPTimeTicks((Long)ZorkaUtil.coerce(val, Long.class));
                case OPAQUE:
                    return null;
                case NSAPADDRESS:
                    if (val instanceof byte[]) {
                        return new SNMPNSAPAddress((byte[])val);
                    } else {
                        return new SNMPNSAPAddress((String)val);
                    }
                case UINTEGER32:
                    return new SNMPUInteger32((Long)ZorkaUtil.coerce(val, Long.class));
            }
        } catch (SNMPBadValueException e) {
            //log.error("Error creating value", e);
        }
        return null;
    }


    public SnmpTrapper trapper(String id) {
        return trappers.get(id);
    }


    public SnmpTrapper trapper(String id, String addr, String community, String agentAddr) {
        return trapper(id, addr, community, agentAddr, SNMP_V1);
    }


    public SnmpTrapper trapper(String id, String addr, String community, String agentAddr, int protocol) {
        SnmpTrapper trapper = trappers.get(id);

        if (trapper == null) {
            trapper = new SnmpTrapper(addr, community, agentAddr, protocol);
            trappers.put(id, trapper);
            trapper.start();
        }

        return trapper;
    }

}
