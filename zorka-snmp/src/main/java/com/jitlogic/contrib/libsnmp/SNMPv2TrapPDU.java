/*
 * SNMP Package
 *
 * Copyright (C) 2004, Jonathan Sevy <jsevy@mcs.drexel.edu>
 *
 * This is free software. Redistribution and use in source and binary forms, with
 * or without modification, are permitted provided that the following conditions
 * are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice, this
 *     list of conditions and the following disclaimer.
 *  2. Redistributions in binary form must reproduce the above copyright notice,
 *     this list of conditions and the following disclaimer in the documentation 
 *     and/or other materials provided with the distribution.
 *  3. The name of the author may not be used to endorse or promote products 
 *     derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED 
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF 
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO 
 * EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT
 * OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS 
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */


package com.jitlogic.contrib.libsnmp;




/**
*    The SNMPv2TrapPDU class represents an SNMPv2 Trap PDU from RFC 1448, as indicated below. This
*    forms the payload of an SNMPv2 Trap message.

-- protocol data units

          3.  Definitions

               SNMPv2-PDU DEFINITIONS ::= BEGIN

               IMPORTS
                   ObjectName, ObjectSyntax, Integer32
                       FROM SNMPv2-SMI;

               -- protocol data units

               PDUs ::=
                   CHOICE {
                       get-request
                           GetRequest-PDU,

                       get-next-request
                           GetNextRequest-PDU,

                       get-bulk-request
                           GetBulkRequest-PDU,

                       response
                           Response-PDU,

                       set-request
                           SetRequest-PDU,

                       inform-request
                           InformRequest-PDU,

                       snmpV2-trap
                           SNMPv2-Trap-PDU
                   }

          
               -- PDUs

               GetRequest-PDU ::=
                   [0]
                       IMPLICIT PDU

               GetNextRequest-PDU ::=
                   [1]
                       IMPLICIT PDU

               Response-PDU ::=
                   [2]
                       IMPLICIT PDU

               SetRequest-PDU ::=
                   [3]
                       IMPLICIT PDU

               -- [4] is obsolete

               GetBulkRequest-PDU ::=
                   [5]
                       IMPLICIT BulkPDU

               InformRequest-PDU ::=
                   [6]
                       IMPLICIT PDU

               SNMPv2-Trap-PDU ::=
                   [7]
                       IMPLICIT PDU

          
               max-bindings
                   INTEGER ::= 2147483647

               PDU ::=
                   SEQUENCE {
                       request-id
                           Integer32,

                       error-status            -- sometimes ignored
                           INTEGER {
                               noError(0),
                               tooBig(1),
                               noSuchName(2),   -- for proxy compatibility
                               badValue(3),     -- for proxy compatibility
                               readOnly(4),     -- for proxy compatibility
                               genErr(5),
                               noAccess(6),
                               wrongType(7),
                               wrongLength(8),
                               wrongEncoding(9),
                               wrongValue(10),
                               noCreation(11),
                               inconsistentValue(12),
                               resourceUnavailable(13),
                               commitFailed(14),
                               undoFailed(15),
                               authorizationError(16),
                               notWritable(17),
                               inconsistentName(18)
                           },

                       error-index            -- sometimes ignored
                           INTEGER (0..max-bindings),

                       variable-bindings   -- values are sometimes ignored
                           VarBindList
                   }

*/


public class SNMPv2TrapPDU extends SNMPPDU
{
    
    
    
    /**
    *    Create a new Trap PDU with given trapOID and sysUptime,
    *    and containing the supplied SNMP sequence as data.
    */
    
    public SNMPv2TrapPDU(SNMPTimeTicks sysUptime, SNMPObjectIdentifier snmpTrapOID, SNMPSequence varList)
        throws SNMPBadValueException
    {
        super(SNMPBERCodec.SNMPv2TRAP, 0, 0, 0, varList);
        
        // create a variable pair for sysUptime, and insert into varBindList
        SNMPObjectIdentifier sysUptimeOID = new SNMPObjectIdentifier("1.3.6.1.2.1.1.3.0");
        SNMPVariablePair sysUptimePair = new SNMPVariablePair(sysUptimeOID, sysUptime);
        varList.insertSNMPObjectAt(sysUptimePair, 0);
        
        // create a variable pair for snmpTrapOID, and insert into varBindList
        SNMPObjectIdentifier snmpTrapOIDOID = new SNMPObjectIdentifier("1.3.6.1.6.3.1.1.4.1.0");
        SNMPVariablePair snmpOIDPair = new SNMPVariablePair(snmpTrapOIDOID, snmpTrapOID);
        varList.insertSNMPObjectAt(snmpOIDPair, 1);
        
    }
    
    
    
    /**
    *    Create a new Trap PDU with given trapOID and sysUptime,
    *    and containing an empty SNMP sequence (VarBindList) as additional data.
    */
    
    public SNMPv2TrapPDU(SNMPObjectIdentifier snmpTrapOID, SNMPTimeTicks sysUptime)
        throws SNMPBadValueException
    {
        this(sysUptime, snmpTrapOID, new SNMPSequence());
    }
    
    
    
    
    /**
    *    Create a new PDU of the specified type from the supplied BER encoding.
    *    @throws SNMPBadValueException Indicates invalid SNMP PDU encoding supplied in enc.
    */
    
    protected SNMPv2TrapPDU(byte[] enc)
        throws SNMPBadValueException
    {
        super(enc, SNMPBERCodec.SNMPv2TRAP);
        
        // validate the message: make sure the first two components of the varBindList
        // are the appropriate variable pairs
        SNMPSequence varBindList = this.getVarBindList();
        
        
        if (varBindList.size() < 2)
        {
            throw new SNMPBadValueException("Bad v2 Trap PDU: missing snmpTrapOID or sysUptime");
        }
        
        // validate that the first variable binding is the sysUptime
        SNMPSequence variablePair = (SNMPSequence)varBindList.getSNMPObjectAt(0);
        SNMPObjectIdentifier oid = (SNMPObjectIdentifier)variablePair.getSNMPObjectAt(0);
        SNMPObject value = variablePair.getSNMPObjectAt(1);
        SNMPObjectIdentifier sysUptimeOID = new SNMPObjectIdentifier("1.3.6.1.2.1.1.3.0");
        if (!(value instanceof SNMPTimeTicks) || !oid.equals(sysUptimeOID))
        {
            throw new SNMPBadValueException("Bad v2 Trap PDU: bad sysUptime in variable binding list");
        }
        
        // validate that the second variable binding is the snmpTrapOID
        variablePair = (SNMPSequence)varBindList.getSNMPObjectAt(1);
        oid = (SNMPObjectIdentifier)variablePair.getSNMPObjectAt(0);
        value = variablePair.getSNMPObjectAt(1);
        SNMPObjectIdentifier snmpTrapOIDOID = new SNMPObjectIdentifier("1.3.6.1.6.3.1.1.4.1.0");
        if (!(value instanceof SNMPObjectIdentifier) || !oid.equals(snmpTrapOIDOID))
        {
            throw new SNMPBadValueException("Bad v2 Trap PDU: bad snmpTrapOID in variable binding list");
        }
        
    }
    
    
    
    
    /** 
    *    A utility method that extracts the snmpTrapOID from the variable bind list (it's the second of the 
    *    variable pairs).
    */
    
    public SNMPObjectIdentifier getSNMPTrapOID()
    {
        SNMPSequence contents = this.getVarBindList();
        SNMPSequence variablePair = (SNMPSequence)contents.getSNMPObjectAt(1);
        return (SNMPObjectIdentifier)variablePair.getSNMPObjectAt(1);
    }
    
    
    
    /** 
    *    A utility method that extracts the sysUptime from the variable bind list (it's the first of the 
    *    variable pairs).
    */
    
    public SNMPTimeTicks getSysUptime()
    {
        SNMPSequence contents = this.getVarBindList();
        SNMPSequence variablePair = (SNMPSequence)contents.getSNMPObjectAt(0);
        return (SNMPTimeTicks)variablePair.getSNMPObjectAt(1);
    }
    
    
}