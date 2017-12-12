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
*    Object representing the SNMP Null data type.
*/

public class SNMPNull extends SNMPObject
{
    
    protected byte tag = SNMPBERCodec.SNMPNULL;
    
    
    /**
    *    Returns Java null reference.
    */
    
    public Object getValue()
    {
        return null;
    }
    
    
    
    /**
    *    Always throws SNMPBadValueException (which null value did you want, anyway?)
    */
    
    public void setValue(Object o)
        throws SNMPBadValueException
    {
        throw new SNMPBadValueException(" Null: attempt to set value ");
    }
    
    
    
    /**
    *    Return BER encoding for a null object: two bytes, tag and length of 0.
    */
    
    protected byte[] getBEREncoding()
    {
        byte[] encoding = new byte[2];
        
        // set tag byte
        encoding[0] = SNMPBERCodec.SNMPNULL;
            
        // len = 0 since no payload!
        encoding[1] = 0;
        
        // no V!
        
        return encoding;
    }
    
    
    
    /**
    *   Checks just that both are instances of SNMPNull (no embedded value to check).
    */
    
    public boolean equals(Object other)
    {
        // false if other is null
        if (other == null)
        {
            return false;
        }
        
        // check that they're both of the same class
        if (this.getClass().equals(other.getClass()))
        {
            return true;
        }
        else
        {
            return false;
        }
    }
    
    
    
    /**
    *    Returns String "Null"..
    */
    
    public String toString()
    {
        return new String("Null");
    }
    
}