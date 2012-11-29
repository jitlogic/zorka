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
*    One of the most important SNMP classes. Represents a sequence of other SNMP data types.
*    Virtually all compound structures are subclasses of SNMPSequence - for example, the 
*    top-level SNMPMessage, and the SNMPPDU it contains, are both just specializations of 
*    SNMPSequence. Sequences are frequently nested within other sequences.
*/

public class SNMPSequence extends SNMPObject
{
    protected Vector sequence;    // Vector of whatever is in sequence
    
    protected byte tag = SNMPBERCodec.SNMPSEQUENCE;
        
    
    /**
    *    Create a new empty sequence.
    */
    
    public SNMPSequence()
    {
        sequence = new Vector();
    }
    
    
    
    
    /**
    *    Create a new SNMP sequence from the supplied Vector of SNMPObjects.
    *    @throws SNMPBadValueException Thrown if non-SNMP object supplied in Vector v.
    */
    
    public SNMPSequence(Vector v)
        throws SNMPBadValueException
    {
        
        Enumeration e = v.elements();
        
        while (e.hasMoreElements())
        {
            if (!(e.nextElement() instanceof SNMPObject))
                throw new SNMPBadValueException("Non-SNMPObject supplied to SNMPSequence.");
        }
        
        
        sequence = v;
    }
    
    
    
    
    /**
    *    Construct an SNMPMessage from a received ASN.1 byte representation.
    *    @throws SNMPBadValueException Indicates invalid SNMP sequence encoding supplied.
    */
    
    protected SNMPSequence(byte[] enc)
        throws SNMPBadValueException
    {
        extractFromBEREncoding(enc);
    }
    
    
    
    
    
    /**
    *    Returns a Vector containing the SNMPObjects in the sequence.
    */
    
    public Object getValue()
    {
        return sequence;
    }
    
    
    
    
    
    /** 
    *    Used to set the contained SNMP objects from a supplied Vector.
    *     @throws SNMPBadValueException Indicates an incorrect object type supplied, or that the supplied
    *    Vector contains non-SNMPObjects.
    */
    
    public void setValue(Object newSequence)
        throws SNMPBadValueException
    {
        if (newSequence instanceof Vector)
        {
            
            // check that all objects in vector are SNMPObjects
            
            Enumeration e = ((Vector)newSequence).elements();
            
            while (e.hasMoreElements())
            {
                if (!(e.nextElement() instanceof SNMPObject))
                    throw new SNMPBadValueException("Non-SNMPObject supplied to SNMPSequence.");
            }
            
            
            this.sequence = (Vector)newSequence;
        }
        else
            throw new SNMPBadValueException(" Sequence: bad object supplied to set value ");
    }
    
    
    
    /** 
    *    Return the number of SNMPObjects contained in the sequence.
    */
    
    public int size()
    {
        return sequence.size();
    }
    
    
    
    /** 
    *    Add the SNMP object to the end of the sequence.
    *    @throws SNMPBadValueException Relevant only in subclasses
    */
    
    public void addSNMPObject(SNMPObject newObject)
        throws SNMPBadValueException
    {
        sequence.insertElementAt(newObject, sequence.size());
    }
    
    
    
    
    /** 
    *    Insert the SNMP object at the specified position in the sequence.
    *    @throws SNMPBadValueException Relevant only in subclasses
    */
    
    public void insertSNMPObjectAt(SNMPObject newObject, int index)
        throws SNMPBadValueException
    {
        sequence.insertElementAt(newObject, index);
    }
    
    
    
    
    
    /** 
    *    Return the SNMP object at the specified index. Indices are 0-based.
    */
    
    public SNMPObject getSNMPObjectAt(int index)
    {
        return (SNMPObject)(sequence.elementAt(index));
    }
    
    
    
    
    /** 
    *    Return the BER encoding for the sequence.
    */
    
    protected byte[] getBEREncoding()
    {
        ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
        
            
        // recursively write contents of Vector
        byte[] data = encodeVector();
        
        // calculate encoding for length of data
        byte[] len = SNMPBERCodec.encodeLength(data.length);
        
        // encode T,L,V info
        outBytes.write(tag);
        outBytes.write(len, 0, len.length);
        outBytes.write(data, 0, data.length);
        
        return outBytes.toByteArray();
    }
    
    
    
    private byte[] encodeVector()
    {
        ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
        
        int numElements = sequence.size();
        for (int i = 0; i < numElements; ++i)
        {
            byte[] nextBytes = ((SNMPObject)(sequence.elementAt(i))).getBEREncoding();
            outBytes.write(nextBytes, 0, nextBytes.length);
        }
        
        return outBytes.toByteArray();
    }
    
    
    
    protected void extractFromBEREncoding(byte[] enc)
        throws SNMPBadValueException
    {
        Vector newVector = new Vector();
        
        int totalLength = enc.length;
        int position = 0;
        
        while (position < totalLength)
        {
            SNMPTLV nextTLV = SNMPBERCodec.extractNextTLV(enc, position);
            newVector.insertElementAt(SNMPBERCodec.extractEncoding(nextTLV), newVector.size());
            position += nextTLV.totalLength;
        }
        
        sequence = newVector;
        
    }
    
    
    
    
    
    /** 
    *    Return a sequence of representations of the contained objects, separated by spaces
    *    and enclosed in parentheses.
    */
    
    public String toString()
    {
        StringBuffer valueStringBuffer = new StringBuffer("(");
        
        
        for (int i = 0; i < sequence.size(); ++i)
        {
            valueStringBuffer.append(" ");
            valueStringBuffer.append(((SNMPObject)sequence.elementAt(i)).toString());
            valueStringBuffer.append(" ");
        }
        
        valueStringBuffer.append(")");
        return valueStringBuffer.toString();
    }
    
    
    
}