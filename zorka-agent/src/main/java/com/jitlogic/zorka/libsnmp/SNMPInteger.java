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



import java.math.*;
import java.io.*;



/** Defines an arbitrarily-sized integer value; there is no limit on size due to the use
* of Java.lang.BigInteger to store the value internally. For an indicator which "pegs" at its 
* maximum value if initialized with a larger value, use SNMPGauge32; for a counter which wraps,
* use SNMPCounter32 or SNMPCounter64.
* @see com.jitlogic.zorka.libsnmp.SNMPCounter32
* @see com.jitlogic.zorka.libsnmp.SNMPGauge32
* @see com.jitlogic.zorka.libsnmp.SNMPCounter64
*/


public class SNMPInteger extends SNMPObject
{
    protected BigInteger value;
    protected byte tag = SNMPBERCodec.SNMPINTEGER;
    
    /** Initialize value to 0.
    */
    
    public SNMPInteger()
    {
        this(0);    // initialize value to 0
    }
    
    
    public SNMPInteger(long value)
    {
        this.value = new BigInteger(new Long(value).toString());
    }
    
    
    public SNMPInteger(BigInteger value)
    {
        this.value = value;
    }
    
    
    
    
    
    /** Used to initialize from the BER encoding, usually received in a response from 
    * an SNMP device responding to an SNMPGetRequest.
    * @throws SNMPBadValueException Indicates an invalid BER encoding supplied. Shouldn't
    * occur in normal operation, i.e., when valid responses are received from devices.
    */
    
    protected SNMPInteger(byte[] enc)
        throws SNMPBadValueException
    {
        extractValueFromBEREncoding(enc);
    }    
    
    
    
    
    /** Returns a java.lang.BigInteger object with the current value.
    */
    
    public Object getValue()
    {
        return value;
    }
    
    
    
    
    
    /** Used to set the value with an instance of java.lang.Integer or
    * java.lang.BigInteger.
    * @throws SNMPBadValueException Indicates an incorrect object type supplied.
    */
    
    public void setValue(Object newValue)
        throws SNMPBadValueException
    {
        if (newValue instanceof BigInteger)
            value = (BigInteger)newValue;
        else if (newValue instanceof Integer)
            value = new BigInteger(((Integer)newValue).toString());
        else if (newValue instanceof String)
            value = new BigInteger((String)newValue);
        else
            
            throw new SNMPBadValueException(" Integer: bad object supplied to set value ");
    }
    
    
    
    
    /** Returns the full BER encoding (type, length, value) of the SNMPInteger subclass.
    */
    
    protected byte[] getBEREncoding()
    {
        ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
        
        // write contents    
        // boy, was THIS easy! Love that Java!
        byte[] data = value.toByteArray();
        
        // calculate encoding for length of data
        byte[] len = SNMPBERCodec.encodeLength(data.length);
        
        // encode T,L,V info
        outBytes.write(tag);
        outBytes.write(len, 0, len.length);
        outBytes.write(data, 0, data.length);
    
        return outBytes.toByteArray();
    }
    
    
    
    
    /** Used to extract a value from the BER encoding of the value. Called in constructors for
    * SNMPInteger subclasses.
    * @throws SNMPBadValueException Indicates an invalid BER encoding supplied. Shouldn't
    * occur in normal operation, i.e., when valid responses are received from devices.
    */
    
    public void extractValueFromBEREncoding(byte[] enc)
        throws SNMPBadValueException
    {
        try
        {
            value = new BigInteger(enc);
        }
        catch (NumberFormatException e)
        {
            throw new SNMPBadValueException(" Integer: bad BER encoding supplied to set value ");
        }
    }
    
    
    
    public String toString()
    {
        return value.toString();
        // return new String(value.toString());
    }
    
    
    
    public String toString(int radix)
    {
        return value.toString(radix);
        // return new String(value.toString());
    }
    
    
    
}