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

import java.io.*;
import java.net.*;


/**
*    The class SNMPInformRequestSenderInterface implements an interface for sending SNMPv2 inform request 
* 	 messages to a remote SNMP manager. The approach is that from version 2c of SNMP, using no encryption of data. 
*    Communication occurs via UDP, port 162, the standard SNMP trap port, as the destination port, unless an
*    alternate (non-standard) port is specified in the constructor.
*/

public class SNMPInformRequestSenderInterface
{
    public static final int SNMP_TRAP_PORT = 162;
    
    private int remotePort = SNMP_TRAP_PORT;
    private DatagramSocket dSocket;
    
    
    /**
    *    Construct a new inform request sender object to send inform requests to remote SNMP hosts.
    */
    
    public SNMPInformRequestSenderInterface()
        throws SocketException
    {
        this(SNMP_TRAP_PORT);
    }
    

    
    /**
    *    Construct a new inform request sender object to send inform requests to remote SNMP hosts, 
    *    using the specified destination port.
    */
    
    public SNMPInformRequestSenderInterface(int remotePort)
        throws SocketException
    {
        this.remotePort = remotePort;
        dSocket = new DatagramSocket();
    }
    

    
    /**
    *    Send the supplied SNMPv2 inform request pdu to the specified host, using the supplied version number
    *    and community name. 
    */
    
    public void sendInformRequest(int version, InetAddress hostAddress, String community, SNMPv2InformRequestPDU pdu)
        throws IOException
    {
        SNMPMessage message = new SNMPMessage(version, community, pdu);
        
        byte[] messageEncoding = message.getBEREncoding();
        
        /*
        System.out.println("Request Message bytes:");
        
        for (int i = 0; i < messageEncoding.length; ++i)
            System.out.print(hexByte(messageEncoding[i]) + " ");
        */
        
        DatagramPacket outPacket = new DatagramPacket(messageEncoding, messageEncoding.length, hostAddress, remotePort);
        
        /*
        System.out.println("Message bytes length (out): " + outPacket.getLength());
        
        System.out.println("Message bytes (out):");
        for (int i = 0; i < messageEncoding.length; ++i)
        {
            System.out.print(hexByte(messageEncoding[i]) + " ");
        }
        System.out.println("\n");
        */
        
        dSocket.send(outPacket);
        
    }
    
    
    
    /**
    *    Send the supplied inform request pdu to the specified host, using the supplied community name and
    *    using 1 for the version field in the SNMP message.
    */
    
    public void sendInformRequest(InetAddress hostAddress, String community, SNMPv2InformRequestPDU pdu)
        throws IOException
    {
        int version = 1;
    
        sendInformRequest(version, hostAddress, community, pdu);
    }
    
    
    
    private String hexByte(byte b)
    {
        int pos = b;
        if (pos < 0)
            pos += 256;
        String returnString = new String();
        returnString += Integer.toHexString(pos/16);
        returnString += Integer.toHexString(pos%16);
        return returnString;
    }
    
    
    
    
    
    
    private String getHex(byte theByte)
    {
        int b = theByte;
        
        if (b < 0)
            b += 256;
        
        String returnString = new String(Integer.toHexString(b));
        
        // add leading 0 if needed
        if (returnString.length()%2 == 1)
            returnString = "0" + returnString;
            
        return returnString;
    }
    
    
    
}