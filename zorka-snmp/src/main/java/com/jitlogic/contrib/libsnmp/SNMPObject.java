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
*    Abstract base class of all SNMP data type classes.
*/


public abstract class SNMPObject
{
    
    /** 
    *    Must return a Java object appropriate to represent the value/data contained
    *     in the SNMP object
    */
    
    public abstract Object getValue();
    
    
    
    /** 
    *    Must set the value of the SNMP object when supplied with an appropriate
    *     Java object containing an appropriate value.
    */
    
    public abstract void setValue(Object o)
        throws SNMPBadValueException;
    
    
    
    /** 
    *    Should return an appropriate human-readable representation of the stored value.
    */
        
    public abstract String toString();
    
    
    
    /** 
    *    Must return the BER byte encoding (type, length, value) of the SNMP object.
    */
        
    protected abstract byte[] getBEREncoding();
    
    
    /**
    *   Compares two SNMPObject subclass objects by checking their values for equality.
    */
    
    public boolean equals(Object other)
    {
        // false if other is null
        if (other == null)
        {
            return false;
        }
        
        // check first to see that they're both of the same class
        if (!this.getClass().equals(other.getClass()))
        {
            return false;
        }
        
        SNMPObject otherSNMPObject = (SNMPObject)other;  
         
        // now see if their embedded values are equal
        if (this.getValue().equals(otherSNMPObject.getValue()))
        {
            return true;
        }
        else
        {
            return false;
        }
    }
    
    
    /**
    *   Generates a hash value so SNMP objects can be used in Hashtables.
    */
    
    public int hashCode()
    {
        // just use hashcode value of embedded value by default
        if (this.getValue() != null)
        {
            return this.getValue().hashCode();
        }
        else
        {
            return 0;
        }
    }
    
    
}