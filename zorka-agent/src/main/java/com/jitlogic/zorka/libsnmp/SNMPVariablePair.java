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




/**
*    The SNMPVariablePair class implements the VarBind specification detailed below from RFC 1157.
*    It is a specialization of SNMPSequence, defining a 2-element sequence containing a single
*    (object identifier, value) pair. Note that the values are themselves SNMPObjects.


          -- variable bindings

          VarBind ::=
                  SEQUENCE {
                      name
                          ObjectName,

                      value
                          ObjectSyntax
                  }



*/


public class SNMPVariablePair extends SNMPSequence
{
    
    /**
    *    Create a new variable pair having the supplied object identifier and vale.
    */
    
    public SNMPVariablePair(SNMPObjectIdentifier objectID, SNMPObject value)
        throws SNMPBadValueException
    {
        super();
        Vector contents = new Vector();
        contents.insertElementAt(objectID, 0);
        contents.insertElementAt(value, 1);
        this.setValue(contents);
    }

    
    
}