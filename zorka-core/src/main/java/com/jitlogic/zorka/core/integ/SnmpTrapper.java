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
import com.jitlogic.zorka.common.stats.AgentDiagnostics;
import com.jitlogic.zorka.common.util.ZorkaLogger;
import com.jitlogic.zorka.common.util.ZorkaAsyncThread;
import com.jitlogic.zorka.common.util.ZorkaLogLevel;
import com.jitlogic.zorka.common.util.ZorkaTrapper;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.List;


/**
 * Sends SNMP traps to remote server.
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class SnmpTrapper extends ZorkaAsyncThread<SNMPSequence> implements ZorkaTrapper {

    /**
     * Default UDP port to send traps to
     */
    public static final int DEFAULT_TRAP_PORT = 162;

    /**
     * UDP port traps will be sent to
     */
    private int snmpPort;

    /**
     * IP address SNMP will be sent to
     */
    private InetAddress snmpAddr;

    /**
     * Protocol version (SNMP_v1 or SNMP_v2).
     */
    private int protocol;

    /**
     * SNMP sender (from SNMP library)
     */
    private SNMPTrapSenderInterface sender;

    /**
     * IP address agent advertises itself as
     */
    private SNMPIPAddress agentAddr;

    /**
     * Community ID
     */
    private String community;

    /**
     * Creates SNMP trapper.
     *
     * @param snmpAddr  IP address traps will be sent to
     * @param community community ID
     * @param agentAddr IP address agent advertises itself as
     * @param protocol  SNMP protocol version
     */
    public SnmpTrapper(String snmpAddr, String community, String agentAddr, int protocol) {
        super("snmp-sender");
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
            log.error(ZorkaLogger.ZAG_ERRORS, "Cannot initialize SNMP sender", e);
        }
    }


    /**
     * Sends an SNMP tra
     *
     * @param gtrap general trap type
     * @param strap specific trap type
     * @param oid   enterprise OID
     * @param vars  additional (named) attributes
     */
    public void trap(int gtrap, int strap, SNMPObjectIdentifier oid, SNMPVariablePair... vars) {
        try {
            SNMPTimeTicks timestamp = new SNMPTimeTicks((long) (System.currentTimeMillis() / 10));
            SNMPVarBindList varBindList = new SNMPVarBindList();

            for (SNMPVariablePair var : vars) {
                varBindList.addSNMPObject(var);
            }

            SNMPSequence trap = null;
            if (protocol == SnmpLib.SNMP_V1) {
                trap = new SNMPv1TrapPDU(oid, agentAddr, gtrap, strap, timestamp, varBindList);
            } else if (protocol == SnmpLib.SNMP_V2) {
                trap = new SNMPv2TrapPDU(timestamp, oid, varBindList);
            } else {
                log.error(ZorkaLogger.ZAG_ERRORS, "Unsupported SNMP protocol version: " + protocol);
            }

            if (trap != null) {
                AgentDiagnostics.inc(countTraps, AgentDiagnostics.TRAPS_SUBMITTED);
                if (!submit(trap)) {
                    AgentDiagnostics.inc(countTraps, AgentDiagnostics.TRAPS_DROPPED);
                }
            }

        } catch (Exception e) {
            log.error(ZorkaLogger.ZAG_ERRORS, "Error creating trap object", e);
        }
    }


    @Override
    public void open() {
        try {
            this.sender = new SNMPTrapSenderInterface(snmpPort);
        } catch (SocketException e) {
            handleError("Cannot initialize sender", e);
        }
    }


    @Override
    public void close() {
        sender.close();
        sender = null;
    }


    @Override
    protected void process(List<SNMPSequence> traps) {
        for (SNMPSequence trap : traps) {
            try {
                if (trap instanceof SNMPv1TrapPDU) {
                    sender.sendTrap(snmpAddr, community, (SNMPv1TrapPDU) trap);
                } else if (trap instanceof SNMPv2TrapPDU) {
                    sender.sendTrap(snmpAddr, community, (SNMPv2TrapPDU) trap);
                }
                AgentDiagnostics.inc(countTraps, AgentDiagnostics.TRAPS_SENT);
            } catch (IOException e) {
                log.error(ZorkaLogger.ZAG_ERRORS, "Error sending SNMP trap", e);
            }
        }
    }


    @Override
    public void trap(ZorkaLogLevel logLevel, String tag, String msg, Throwable e, Object... args) {
        // TODO implement this using some "standardized" OID
    }
}
