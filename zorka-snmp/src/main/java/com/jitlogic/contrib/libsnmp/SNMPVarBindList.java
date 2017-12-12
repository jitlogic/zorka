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

import java.util.*;




/**
*    The SNMPVarBindList class is a specialization of SNMPSequence that contains a list of
*    SNMPVariablePair objects.
*    @see com.jitlogic.contrib.libsnmp.SNMPVariablePair

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

public class SNMPVarBindList extends SNMPSequence
{
    
    /** 
    *    Create a new empty variable binding list.
    */
    
    
    public SNMPVarBindList()
    {
        super();
    }
    
    
    
    
    
    /**
    *    Return the variable pairs in the list, separated by spaces.
    */
    
    public String toString()
    {
        Vector sequence = (Vector)(this.getValue());
        
        StringBuffer valueStringBuffer = new StringBuffer();
        
        for (int i = 0; i < sequence.size(); ++i)
        {
            valueStringBuffer.append(((SNMPObject)sequence.elementAt(i)).toString());
            valueStringBuffer.append(" ");
        }
        
        return valueStringBuffer.toString();
    }
    
    
    
}