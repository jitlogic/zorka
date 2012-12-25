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

import com.jitlogic.contrib.libsnmp.*;
import com.jitlogic.zorka.util.ZorkaAsyncThread;
import com.jitlogic.zorka.util.ZorkaLog;
import com.jitlogic.zorka.util.ZorkaLogger;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;


/**
 *
 */
public class SnmpTrapper extends ZorkaAsyncThread<SNMPSequence> {

    public static final int DEFAULT_TRAP_PORT = 162;

    private int snmpPort, protocol;
    private InetAddress snmpAddr;

    private SNMPTrapSenderInterface trapper;

    private SNMPIPAddress agentAddr;
    private String community;


    public SnmpTrapper(String snmpAddr, String community, String agentAddr, int protocol) {
        super("snmp-trapper");
        try {
            if (snmpAddr.contains(":")) {
                String[] s = snmpAddr.split(":");
                this.snmpAddr = InetAddress.getByName(s[0]);
                this.snmpPort = Integer.parseInt(s[1]);
            } else {
                this.snmpAddr = InetAddress.getByName(snmpAddr);
                this.snmpPort = DEFAULT_TRAP_PORT;
            }

            this.community = community;
            this.agentAddr = new SNMPIPAddress(agentAddr);
            this.protocol = protocol;
        } catch (Exception e) {
            log.error("Cannot initialize SNMP trapper", e);
        }

        log = ZorkaLogger.getLog(this.getClass());
    }


    public void trap(int gtrap, int strap, SNMPObjectIdentifier oid, SNMPVariablePair...vars) {
        try {
            SNMPTimeTicks timestamp = new SNMPTimeTicks((long)(System.currentTimeMillis()/10));
            SNMPVarBindList varBindList = new SNMPVarBindList();

            for (SNMPVariablePair var : vars) {
                varBindList.addSNMPObject(var);
            }

            if (protocol == SnmpLib.SNMP_V1) {
                submit(new SNMPv1TrapPDU(oid, agentAddr, gtrap, strap, timestamp, varBindList));
            } else if (protocol == SnmpLib.SNMP_V2) {
                submit(new SNMPv2TrapPDU(timestamp, oid, varBindList));
            } else {
                log.error("Unsupported SNMP protocol version: " + protocol);
            }
        } catch (Exception e) {
            log.error("Error creating trap object", e);
        }
    }


    protected void open() {
        try {
            this.trapper = new SNMPTrapSenderInterface(snmpPort);
        } catch (SocketException e) {
            handleError("Cannot initialize trapper", e);
        }
    }

    public void close() {
        trapper.close();
        trapper = null;
    }


    @Override
    protected void process(SNMPSequence trap) {
        try {
            if (trap instanceof SNMPv1TrapPDU) {
                trapper.sendTrap(snmpAddr, community, (SNMPv1TrapPDU)trap);
            } else if (trap instanceof SNMPv2TrapPDU) {
                trapper.sendTrap(snmpAddr, community, (SNMPv2TrapPDU)trap);
            }
        } catch (IOException e) {
            log.error("Error sending SNMP trap", e);
        }
    }

}
