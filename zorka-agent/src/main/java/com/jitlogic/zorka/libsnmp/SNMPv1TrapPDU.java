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


package com.jitlogic.zorka.libsnmp;

import java.util.*;
import java.math.*;




/**
*    The SNMPTrapPDU class represents an SNMPv1 Trap PDU from RFC 1157, as indicated below. This
*    forms the payload of an SNMP Trap message.

-- protocol data units

          PDUs ::=
                  CHOICE {
                              get-request
                                  GetRequest-PDU,

                              get-next-request
                                  GetNextRequest-PDU,

                              get-response
                                  GetResponse-PDU,

                              set-request
                                  SetRequest-PDU,

                              trap
                                  Trap-PDU
                          }

          -- PDUs

          GetRequest-PDU ::=
              [0]
                  IMPLICIT PDU

          GetNextRequest-PDU ::=
              [1]
                  IMPLICIT PDU

          GetResponse-PDU ::=
              [2]
                  IMPLICIT PDU

          SetRequest-PDU ::=
              [3]
                  IMPLICIT PDU

          PDU ::=
                  SEQUENCE {
                     request-id
                          INTEGER,

                      error-status      -- sometimes ignored
                          INTEGER {
                              noError(0),
                              tooBig(1),
                              noSuchName(2),
                              badValue(3),
                              readOnly(4),
                              genErr(5)
                          },

                      error-index       -- sometimes ignored
                         INTEGER,

                      variable-bindings -- values are sometimes ignored
                          VarBindList
                  }

          
          Trap-PDU ::=
              [4]
                 IMPLICIT SEQUENCE {
                      enterprise        -- type of object generating
                                        -- trap, see sysObjectID in [5]

                          OBJECT IDENTIFIER,

                      agent-addr        -- address of object generating
                          NetworkAddress, -- trap

                      generic-trap      -- generic trap type
                          INTEGER {
                              coldStart(0),
                              warmStart(1),
                              linkDown(2),
                              linkUp(3),
                              authenticationFailure(4),
                              egpNeighborLoss(5),
                              enterpriseSpecific(6)
                          },

                      specific-trap  -- specific code, present even
                          INTEGER,   -- if generic-trap is not
                                     -- enterpriseSpecific

                      time-stamp     -- time elapsed between the last
                          TimeTicks, -- (re)initialization of the
                                        network
                                     -- entity and the generation of the
                                        trap

                       variable-bindings -- "interesting" information
                          VarBindList
                  }
          -- variable bindings

          VarBind ::=
                  SEQUENCE {
                      name
                          ObjectName,

                      value
                          ObjectSyntax
                  }

         VarBindList ::=
                  SEQUENCE OF
                     VarBind

         END

*/


public class SNMPv1TrapPDU extends SNMPSequence
{
    
    /**
    *    Create a new Trap PDU of the specified type, with given request ID, error status, and error index,
    *    and containing the supplied SNMP sequence as data.
    */
    
    public SNMPv1TrapPDU(SNMPObjectIdentifier enterpriseOID, SNMPIPAddress agentAddress, int genericTrap, int specificTrap, SNMPTimeTicks timestamp, SNMPSequence varList)
        throws SNMPBadValueException
    {
        super();
        
        tag = SNMPBERCodec.SNMPTRAP;
        
        Vector contents = new Vector();
        
        contents.addElement(enterpriseOID);
        contents.addElement(agentAddress);
        contents.addElement(new SNMPInteger(genericTrap));
        contents.addElement(new SNMPInteger(specificTrap));
        contents.addElement(timestamp);
        contents.addElement(varList);
        
        this.setValue(contents);
    }
    
    
    
    /**
    *    Create a new Trap PDU of the specified type, with given request ID, error status, and error index,
    *    and containing an empty SNMP sequence (VarBindList) as additional data.
    */
    
    public SNMPv1TrapPDU(SNMPObjectIdentifier enterpriseOID, SNMPIPAddress agentAddress, int genericTrap, int specificTrap, SNMPTimeTicks timestamp)
        throws SNMPBadValueException
    {
        super();
        
        tag = SNMPBERCodec.SNMPTRAP;
        
        Vector contents = new Vector();
        
        contents.addElement(enterpriseOID);
        contents.addElement(agentAddress);
        contents.addElement(new SNMPInteger(genericTrap));
        contents.addElement(new SNMPInteger(specificTrap));
        contents.addElement(timestamp);
        contents.addElement(new SNMPVarBindList());
        
        this.setValue(contents);
    }
    
    
    
    
    /**
    *    Create a new PDU of the specified type from the supplied BER encoding.
    *    @throws SNMPBadValueException Indicates invalid SNMP PDU encoding supplied in enc.
    */
    
    protected SNMPv1TrapPDU(byte[] enc)
        throws SNMPBadValueException
    {
        tag = SNMPBERCodec.SNMPTRAP;
        extractFromBEREncoding(enc);
        
        // validate the message: make sure we have the appropriate pieces
        Vector contents = (Vector)(this.getValue());
        
        if (contents.size() != 6)
        {
            throw new SNMPBadValueException("Bad Trap PDU");
        }
        
        if (!(contents.elementAt(0) instanceof SNMPObjectIdentifier))
        {
            throw new SNMPBadValueException("Bad Trap PDU: bad enterprise OID");
        }
        
        if (!(contents.elementAt(1) instanceof SNMPIPAddress))
        {
            throw new SNMPBadValueException("Bad Trap PDU: bad agent address");
        }
        
        if (!(contents.elementAt(2) instanceof SNMPInteger))
        {
            throw new SNMPBadValueException("Bad Trap PDU: bad generic trap code");
        }
        
        if (!(contents.elementAt(3) instanceof SNMPInteger))
        {
            throw new SNMPBadValueException("Bad Trap PDU: bad specific trap code");
        }
        
        if (!(contents.elementAt(4) instanceof SNMPTimeTicks))
        {
            throw new SNMPBadValueException("Bad Trap PDU: bad timestamp");
        }
        
        if (!(contents.elementAt(5) instanceof SNMPSequence))
        {
            throw new SNMPBadValueException("Bad Trap PDU: bad variable binding list");
        }
        
        // now validate the variable binding list: should be list of sequences which
        // are (OID, value) pairs
        SNMPSequence varBindList = this.getVarBindList();
        for (int i = 0; i < varBindList.size(); i++)
        {
            SNMPObject element = varBindList.getSNMPObjectAt(i);
            
            // must be a two-element sequence
            if (!(element instanceof SNMPSequence))
            {
                throw new SNMPBadValueException("Bad Trap PDU: bad variable binding at index" + i);
            }
            
            // variable binding sequence must have 2 elements, first of which must be an object identifier
            SNMPSequence varBind = (SNMPSequence)element;
            if ((varBind.size() != 2) || !(varBind.getSNMPObjectAt(0) instanceof SNMPObjectIdentifier))
            {
                throw new SNMPBadValueException("Bad Trap PDU: bad variable binding at index" + i);
            }
        }
    }
    
    
    
    
    /** 
    *    A utility method that extracts the variable binding list from the pdu. Useful for retrieving
    *    the set of (object identifier, value) pairs returned in response to a request to an SNMP
    *    device. The variable binding list is just an SNMP sequence containing the identifier, value pairs.
    *    @see com.jitlogic.zorka.libsnmp.SNMPVarBindList
    */
    
    public SNMPSequence getVarBindList()
    {
        Vector contents = (Vector)(this.getValue());
        return (SNMPSequence)(contents.elementAt(5));
    }
    
    
    
    /** 
    *    A utility method that extracts the enterprise OID from this PDU.
    */
    
    public SNMPObjectIdentifier getEnterpriseOID()
    {
        Vector contents = (Vector)(this.getValue());
        return (SNMPObjectIdentifier)contents.elementAt(0);
    }
    
    
    
    /** 
    *    A utility method that extracts the sending agent address this PDU.
    */
    
    public SNMPIPAddress getAgentAddress()
    {
        Vector contents = (Vector)(this.getValue());
        return (SNMPIPAddress)contents.elementAt(1);
    }
    
    
    
    /** 
    *    A utility method that returns the generic trap code for this PDU.
    */
    
    public int getGenericTrap()
    {
        Vector contents = (Vector)(this.getValue());
        return ((BigInteger)((SNMPInteger)(contents.elementAt(2))).getValue()).intValue();
    }
    
    
    /** 
    *    A utility method that returns the specific trap code for this PDU.
    */
    
    public int getSpecificTrap()
    {
        Vector contents = (Vector)(this.getValue());
        return ((BigInteger)((SNMPInteger)(contents.elementAt(3))).getValue()).intValue();
    }
    
    
    /** 
    *    A utility method that returns the timestamp for this PDU.
    */
    
    public long getTimestamp()
    {
        Vector contents = (Vector)(this.getValue());
        return ((BigInteger)((SNMPTimeTicks)(contents.elementAt(4))).getValue()).longValue();
    }
    
    
}