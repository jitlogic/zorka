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
import java.io.*;

/**
*    Class representing ASN.1 object identifiers. These are unbounded sequences (arrays) of
*    natural numbers, written as dot-separated strings.
*/

public class SNMPObjectIdentifier extends SNMPObject
{
    private long[] digits;    // array of longs
    
    protected byte tag = SNMPBERCodec.SNMPOBJECTIDENTIFIER;    
    
    /**
    *    Create a new empty object identifier (0-length array). 
    */
    
    public SNMPObjectIdentifier()
    {    
        digits = new long[0];
    }
    
    
    
    
    /**
    *    Create a new object identifier from the supplied string of dot-separated nonegative
    *    decimal integer values.
    *    @throws SNMPBadValueException Indicates incorrectly-formatted string supplied.
    */
    
    public SNMPObjectIdentifier(String digitString)
        throws SNMPBadValueException
    {
        convertDigitString(digitString);
    }
    
    
    
    
    /**
    *    Create a new object identifier from the supplied array of nonegative
    *    integer values.
    *    @throws SNMPBadValueException Negative value(s) supplied.
    */
    
    public SNMPObjectIdentifier(int[] digits)
        throws SNMPBadValueException
    {
        long[] longDigits = new long[digits.length];
        
        for (int i = 0; i < digits.length; i++)
        {
            if (digits[i] < 0)
                throw new SNMPBadValueException("Negative value supplied for SNMPObjectIdentifier.");
            
            longDigits[i] = digits[i];
        }
                
        this.digits = longDigits;
    }
    
    
    
    
    /**
    *    Create a new object identifier from the supplied array of nonegative
    *    long values.
    *    @throws SNMPBadValueException Negative value(s) supplied.
    */
    
    public SNMPObjectIdentifier(long[] digits)
        throws SNMPBadValueException
    {
        
        for (int i = 0; i < digits.length; i++)
        {
            if (digits[i] < 0)
                throw new SNMPBadValueException("Negative value supplied for SNMPObjectIdentifier.");
        }
                
        this.digits = digits;
    }
    
    
    
    
    /** 
    *    Used to initialize from the BER encoding, as received in a response from 
    *     an SNMP device responding to an SNMPGetRequest.
    *     @throws SNMPBadValueException Indicates an invalid BER encoding supplied. Shouldn't
    *     occur in normal operation, i.e., when valid responses are received from devices.
    */
    
    protected SNMPObjectIdentifier(byte[] enc)
        throws SNMPBadValueException
    {
        extractFromBEREncoding(enc);
    }
    
    
    
    
    /** 
    *    Return array of integers corresponding to components of identifier.
    */
    
    public Object getValue()
    {
        return digits;
    }
    
    
    
    
    
    /** 
    *    Used to set the value from an integer or long array containing the identifier components, or from
    *    a String containing a dot-separated sequence of nonegative values.
    *     @throws SNMPBadValueException Indicates an incorrect object type supplied, or negative array
    *    elements, or an incorrectly formatted String.
    */
    
    public void setValue(Object digits)
        throws SNMPBadValueException
    {
        if (digits instanceof long[])
        {
            for (int i = 0; i < ((long[])digits).length; i++)
            {
                if (((long[])digits)[i] < 0)
                    throw new SNMPBadValueException("Negative value supplied for SNMPObjectIdentifier.");
            }
            
            this.digits = (long[])digits;
        }
        else if (digits instanceof int[])
        {
            long[] longDigits = new long[((int[])digits).length];
        
            for (int i = 0; i < ((int[])digits).length; i++)
            {
                if (((int[])digits)[i] < 0)
                    throw new SNMPBadValueException("Negative value supplied for SNMPObjectIdentifier.");
                
                longDigits[i] = ((int[])digits)[i];
            }
                
            this.digits = longDigits;
        }
        else if (digits instanceof String)
        {
            convertDigitString((String)digits);
        }
        else
            throw new SNMPBadValueException(" Object Identifier: bad object supplied to set value ");
    }
    
    
    
    
    /**
    *    Return BER encoding for this object identifier.
    */
    
    protected byte[] getBEREncoding()
    {
        ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
        
        byte type = SNMPBERCodec.SNMPOBJECTIDENTIFIER;
            
        // write contents of array of values
        byte[] data = encodeArray();
        
        // calculate encoding for length of data
        byte[] len = SNMPBERCodec.encodeLength(data.length);
        
        // encode T,L,V info
        outBytes.write(type);
        outBytes.write(len, 0, len.length);
        outBytes.write(data, 0, data.length);
        
        return outBytes.toByteArray();
    }
    
    
    
    private byte[] encodeArray()
    {
        ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
        
        int numElements = digits.length;
        
        // encode first two identifier digits as one byte, using the 40*x + y rule;
        // of course, if only one element, just use 40*x; if none, do nothing
        if (numElements >= 2)
        {
            outBytes.write((byte)(40*digits[0] + digits[1]));
        }
        else if (numElements ==1)
        {
            outBytes.write((byte)(40*digits[0]));
        }
        
        
        for (int i = 2; i < numElements; ++i)
        {
            byte[] nextBytes = encodeValue(digits[i]);
            outBytes.write(nextBytes, 0, nextBytes.length);
        }
        
        
        return outBytes.toByteArray();
    }
    
    
    
    private byte[] encodeValue(long v)
    {
        // see how many bytes are needed: each value uses just
        // 7 bits of each byte, with high-order bit functioning as
        // a continuation marker
        int numBytes = 0;
        long temp = v;
        
        do
        {
            ++numBytes;
            temp = (long)Math.floor(temp / 128);
        }
        while (temp > 0);
        
        
        byte[] enc = new byte[numBytes];
        // encode lowest-order byte, without setting high bit
        enc[numBytes-1] = (byte)(v % 128);
        v = (long)Math.floor(v / 128);
        
        //.encode other bytes with high bit set
        for (int i = numBytes-2; i >= 0; --i)
        {
            enc[i] = (byte)((v % 128) + 128);
            v = (long)Math.floor(v / 128);
        }
        
        return enc;
    }
    
    
    
    private void convertDigitString(String digitString)
        throws SNMPBadValueException
    {
        try
        {
            StringTokenizer st = new StringTokenizer(digitString, " .");
            int size = 0;
            
            while (st.hasMoreTokens())
            {
                // figure out how many values are in string
                size++;
                st.nextToken();
            }
            
            long[] returnDigits = new long[size];
            
            st = new StringTokenizer(digitString, " .");
            
            for (int i = 0; i < size; i++)
            {
                returnDigits[i] = Long.parseLong(st.nextToken());
                if (returnDigits[i] < 0)
                    throw new SNMPBadValueException(" Object Identifier: bad string supplied to set value ");
            }
            
            digits = returnDigits;
            
        }
        catch (NumberFormatException e)
        {
            throw new SNMPBadValueException(" Object Identifier: bad string supplied for object identifier value ");
        }
        
        
    }
    
    
    
    
    private void extractFromBEREncoding(byte[] enc)
        throws SNMPBadValueException
    {
        // note: masks must be ints; byte internal representation issue(?)
        int bitTest = 0x80;    // test for leading 1
        int highBitMask = 0x7F;    // mask out high bit for value
        
        // first, compute number of "digits";
        // will just be number of bytes with leading 0's
        int numInts = 0;
        for (int i = 0; i < enc.length; i++)
        {
            if ((enc[i] & bitTest) == 0)        //high-order bit not set; count
                numInts++;
        }
        
        
        if (numInts > 0)
        {
            // create new int array to hold digits; since first value is 40*x + y,
            // need one extra entry in array to hold this.
            digits = new long[numInts + 1];    
            
            int currentByte = -1;    // will be incremented to 0
            
            long value = 0;
            
            // read in values 'til get leading 0 in byte
            do
            {
                currentByte++;
                value = value*128 + (enc[currentByte] & highBitMask);
            }
            while ((enc[currentByte] & bitTest) > 0);    // implies high bit set!
            
            // now handle 40a + b
            digits[0] = (long)Math.floor(value / 40);
            digits[1] = value % 40;
            
            // now read in rest!
            for (int i = 2; i < numInts + 1; i++)
            {
                // read in values 'til get leading 0 in byte
                value = 0;
                do
                {
                    currentByte++;
                    value = value*128 + (enc[currentByte] & highBitMask);
                }
                while ((enc[currentByte] & bitTest) > 0);
                
                digits[i] = value;
            }
            
        }
        else
        {
            // no digits; create empty digit array
            digits = new long[0];
        }
        
    }
    
    
    
    
    /*
    public boolean equals(SNMPObjectIdentifier other)
    {
        long[] otherDigits = (long[])(other.getValue());
        
        boolean areEqual = true;
        
        if (digits.length != otherDigits.length)
        {
            areEqual = false;
        }
        else
        {
            for (int i = 0; i < digits.length; i++)
            {
                if (digits[i] != otherDigits[i])
                {
                    areEqual = false;
                    break;
                }
            }
        }
        
        return areEqual;
            
    }
    */
    
    
    
    
    /**
    *   Checks the internal arrays for equality.
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
        
        SNMPObjectIdentifier otherSNMPObject = (SNMPObjectIdentifier)other; 
         
        // see if their embedded arrays are equal
        if (java.util.Arrays.equals((long[])this.getValue(),(long[])otherSNMPObject.getValue()))
        {
            return true;
        }
        else
        {
            return false;
        }
    }
    
    
    
    /**
    *   Generates a hash value so SNMP Object Identifiers can be used in Hashtables.
    */
    
    public int hashCode()
    {
        int hash = 0;
        
        // generate a hashcode from the embedded array
        for (int i = 0; i < digits.length; i++)
        {
            hash += (int)(digits[i] ^ (digits[i] >> 32));
            hash += (hash << 10);
            hash ^= (hash >> 6);
        }
        
        hash += (hash << 3);
        hash ^= (hash >> 11);
        hash += (hash << 15);
         
        return hash;
    }
    
    
    
    /**
    *    Return dot-separated sequence of decimal values.
    */
    
    public String toString()
    {
        StringBuffer valueStringBuffer = new StringBuffer();
        if (digits.length > 0)
        {
            valueStringBuffer.append(digits[0]);
            
            for (int i = 1; i < digits.length; ++i)
            {
                valueStringBuffer.append(".");
                valueStringBuffer.append(digits[i]);
            }
        }
        
            
        return valueStringBuffer.toString();
    }
    
    
    
    
}