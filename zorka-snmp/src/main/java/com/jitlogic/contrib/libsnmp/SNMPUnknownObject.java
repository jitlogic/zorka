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

import java.io.*;



/**
*    Used when an unknown SNMP object type is encountered. Just takes a byte array
*    for its constructor, and uses this as raw bytes.
*/

public class SNMPUnknownObject extends SNMPObject
{
    private byte[] data;
    
    protected byte tag = SNMPBERCodec.SNMPUNKNOWNOBJECT;    
    
    /**
    *    Just takes a byte array, and uses this as raw bytes.
    */
    public SNMPUnknownObject(byte[] enc)
    {
        data = enc;
    }
    
    
    
    
    /**
    *    Return a byte array containing the raw bytes supplied.
    */
    public Object getValue()
    {
        return data;
    }
    
    
    
    
    /**
    *    Takes a byte array containing the raw bytes stored as the value.
    */
    
    public void setValue(Object data)
        throws SNMPBadValueException
    {
        if (data instanceof byte[])
            this.data = (byte[])data;
        else
            throw new SNMPBadValueException(" Unknown Object: bad object supplied to set value ");
    }
    
    
    
    
    
    /**
    *    Return the BER encoding of this object.
    */
    
    protected byte[] getBEREncoding()
    {
        
        ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
        
        byte type = SNMPBERCodec.SNMPUNKNOWNOBJECT;
        
        // calculate encoding for length of data
        byte[] len = SNMPBERCodec.encodeLength(data.length);
        
        // encode T,L,V info
        outBytes.write(type);
        outBytes.write(len, 0, len.length);
        outBytes.write(data, 0, data.length);
    
        return outBytes.toByteArray();
    }
    
    
    
    
    /**
    *    Return String created from raw bytes of this object.
    */
    
    public String toString()
    {
        return new String(data);
    }
    
    
    
}