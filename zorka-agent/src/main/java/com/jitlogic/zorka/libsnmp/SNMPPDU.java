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
*    The SNMPPDU class represents an SNMP PDU from RFC 1157, as indicated below. This
*    forms the payload of an SNMP message.

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


public class SNMPPDU extends SNMPSequence
{
    
    
    /**
    *    Create a new PDU of the specified type, with given request ID, error status, and error index,
    *    and containing the supplied SNMP sequence as data.
    */
    
    public SNMPPDU(byte pduType, int requestID, int errorStatus, int errorIndex, SNMPSequence varList)
        throws SNMPBadValueException
    {
        super();
        Vector contents = new Vector();
        tag = pduType;
        contents.insertElementAt(new SNMPInteger(requestID), 0);
        contents.insertElementAt(new SNMPInteger(errorStatus), 1);
        contents.insertElementAt(new SNMPInteger(errorIndex), 2);
        contents.insertElementAt(varList, 3);
        this.setValue(contents);
    }
    
    
    
    
    /**
    *    Create a new PDU of the specified type from the supplied BER encoding.
    *    @throws SNMPBadValueException Indicates invalid SNMP PDU encoding supplied in enc.
    */
    
    protected SNMPPDU(byte[] enc, byte pduType)
        throws SNMPBadValueException
    {
        tag = pduType;
        extractFromBEREncoding(enc);
        
        // validate the message: make sure we have the appropriate pieces
        Vector contents = (Vector)(this.getValue());
        
        if (contents.size() != 4)
        {
            throw new SNMPBadValueException("Bad PDU");
        }
        
        if (!(contents.elementAt(0) instanceof SNMPInteger))
        {
            throw new SNMPBadValueException("Bad PDU: bad request ID");
        }
        
        if (!(contents.elementAt(1) instanceof SNMPInteger))
        {
            throw new SNMPBadValueException("Bad PDU: bad error status");
        }
        
        if (!(contents.elementAt(2) instanceof SNMPInteger))
        {
            throw new SNMPBadValueException("Bad PDU: bad error index");
        }
        
        if (!(contents.elementAt(3) instanceof SNMPSequence))
        {
            throw new SNMPBadValueException("Bad PDU: bad variable binding list");
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
                throw new SNMPBadValueException("Bad PDU: bad variable binding at index" + i);
            }
            
            // variable binding sequence must have 2 elements, first of which must be an object identifier
            SNMPSequence varBind = (SNMPSequence)element;
            if ((varBind.size() != 2) || !(varBind.getSNMPObjectAt(0) instanceof SNMPObjectIdentifier))
            {
                throw new SNMPBadValueException("Bad PDU: bad variable binding at index" + i);
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
        return (SNMPSequence)(contents.elementAt(3));
    }
    
    
    
    /** 
    *    A utility method that extracts the request ID number from this PDU.
    */
    
    public int getRequestID()
    {
        Vector contents = (Vector)(this.getValue());
        return ((BigInteger)((SNMPInteger)(contents.elementAt(0))).getValue()).intValue();
    }
    
    
    
    /** 
    *    A utility method that extracts the error status for this PDU; if nonzero, can get index of
    *    problematic variable using getErrorIndex().
    */
    
    public int getErrorStatus()
    {
        Vector contents = (Vector)(this.getValue());
        return ((BigInteger)((SNMPInteger)(contents.elementAt(1))).getValue()).intValue();
    }
    
    
    
    /** 
    *    A utility method that returns the error index for this PDU, identifying the problematic variable.
    */
    
    public int getErrorIndex()
    {
        Vector contents = (Vector)(this.getValue());
        return ((BigInteger)((SNMPInteger)(contents.elementAt(2))).getValue()).intValue();
    }
    
    
    
    /** 
    *    A utility method that returns the PDU type of this PDU.
    */
    
    public byte getPDUType()
    {
        return tag;
    }
    
    
}