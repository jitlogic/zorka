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
import java.net.*;


/**
*    The class SNMPv1CommunicationInterface defines methods for communicating with SNMP entities.
*    The approach is that from version 1 of SNMP, using no encryption of data. Communication occurs
*    via UDP, using port 161, the standard SNMP port, unless an alternate (non-standard) port is
*    supplied in the constructor.
*/

public class SNMPv1CommunicationInterface
{
    public static final int SNMP_PORT = 161;
    
    // port to which requests will be sent
    private int remotePort = SNMP_PORT;
    
    // largest size for datagram packet payload; based on
    // RFC 1157, need to handle messages of at least 484 bytes
    private int receiveBufferSize = 512;
    
    private int version;
    private InetAddress hostAddress;
    private String community;
    DatagramSocket dSocket;
    
    public int requestID = 1;
            
    
    
    
    /**
    *    Construct a new communication object to communicate with the specified host using the
    *    given community name. The version setting should be either 0 (version 1) or 1 (version 2,
    *    a la RFC 1157).
    */
    
    public SNMPv1CommunicationInterface(int version, InetAddress hostAddress, String community)
        throws SocketException
    {
        this(version, hostAddress, community, SNMP_PORT);
    }
    
    
    
    /**
     *    Construct a new communication object to communicate with the specified host using the
     *    given community name, and sending requests to the specified port. The version setting 
     *    should be either 0 (version 1) or 1 (version 2, a la RFC 1157).
     */
     
     public SNMPv1CommunicationInterface(int version, InetAddress hostAddress, String community, int remotePort)
         throws SocketException
     {
         this.remotePort = remotePort;
         this.version = version;
         this.hostAddress = hostAddress;
         this.community = community;
         
         dSocket = new DatagramSocket();
         dSocket.setSoTimeout(15000);    //15 seconds
     }
    
    
    
    
    /**
    *    Permits setting timeout value for underlying datagram socket (in milliseconds).
    */
    
    public void setSocketTimeout(int socketTimeout)
        throws SocketException
    {
        dSocket.setSoTimeout(socketTimeout);
    }
    
    
    
    /**
    *    Close the "connection" with the device.
    */
    
    public void closeConnection()
        throws SocketException
    {
        dSocket.close();
    }

    
    
    
    
    
    /**
    *    Retrieve all MIB variable values subsequent to the starting object identifier
    *    given in startID (in dotted-integer notation). Return as SNMPVarBindList object.
    *    Uses SNMPGetNextRequests to retrieve variable values in sequence.
    *    @throws IOException Thrown when timeout experienced while waiting for response to request.
    *    @throws SNMPBadValueException 
    */
    
    public SNMPVarBindList retrieveAllMIBInfo(String startID)
        throws IOException, SNMPBadValueException
    {
        // send GetNextRequests until receive
        // an error message or a repeat of the object identifier we sent out
        SNMPVarBindList retrievedVars = new SNMPVarBindList();
        
        
        int errorStatus = 0;
        int errorIndex = 0;
        
        SNMPObjectIdentifier requestedObjectIdentifier = new SNMPObjectIdentifier(startID);
        SNMPVariablePair nextPair = new SNMPVariablePair(requestedObjectIdentifier, new SNMPNull());
        SNMPSequence varList = new SNMPSequence();
        varList.addSNMPObject(nextPair);
        SNMPPDU pdu = new SNMPPDU(SNMPBERCodec.SNMPGETNEXTREQUEST, requestID, errorStatus, errorIndex, varList);
        SNMPMessage message = new SNMPMessage(version, community, pdu);
        byte[] messageEncoding = message.getBEREncoding();
        DatagramPacket outPacket = new DatagramPacket(messageEncoding, messageEncoding.length, hostAddress, remotePort);
        
        
        dSocket.send(outPacket);
        
        
        
        while (errorStatus == 0)
        {
            
            DatagramPacket inPacket = new DatagramPacket(new byte[receiveBufferSize], receiveBufferSize);
        
            dSocket.receive(inPacket);
            
            byte[] encodedMessage = inPacket.getData();
            
            
            SNMPMessage receivedMessage = new SNMPMessage(SNMPBERCodec.extractNextTLV(encodedMessage,0).value);
            //errorStatus = ((BigInteger)((SNMPInteger)((receivedMessage.getPDU()).getSNMPObjectAt(1))).getValue()).intValue();
            
            
            varList = (receivedMessage.getPDU()).getVarBindList();
            SNMPSequence newPair = (SNMPSequence)(varList.getSNMPObjectAt(0));
            
            SNMPObjectIdentifier newObjectIdentifier = (SNMPObjectIdentifier)(newPair.getSNMPObjectAt(0));
            SNMPObject newValue = newPair.getSNMPObjectAt(1);
            
            retrievedVars.addSNMPObject(newPair);
            
            
            if (requestedObjectIdentifier.equals(newObjectIdentifier))
                break;
                
            requestedObjectIdentifier = newObjectIdentifier;
        
            requestID++;
            nextPair = new SNMPVariablePair(requestedObjectIdentifier, new SNMPNull());
            varList = new SNMPSequence();
            varList.addSNMPObject(nextPair);
            pdu = new SNMPPDU(SNMPBERCodec.SNMPGETNEXTREQUEST, requestID, errorStatus, errorIndex, varList);
            message = new SNMPMessage(version, community, pdu);
            messageEncoding = message.getBEREncoding();
            outPacket = new DatagramPacket(messageEncoding, messageEncoding.length, hostAddress, remotePort);
            
            dSocket.send(outPacket);
            
        }
            
        
        return retrievedVars;
        
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
    
    
    
    
    
    /**
    *    Retrieve the MIB variable value corresponding to the object identifier
    *    given in itemID (in dotted-integer notation). Return as SNMPVarBindList object; if no
    *    such variable (either due to device not supporting it, or community name having incorrect
    *    access privilege), SNMPGetException thrown
    *    @throws IOException Thrown when timeout experienced while waiting for response to request.
    *    @throws SNMPBadValueException 
    *   @throws SNMPGetException Thrown if supplied OID has value that can't be retrieved
    */
    
    public SNMPVarBindList getMIBEntry(String itemID)
        throws IOException, SNMPBadValueException, SNMPGetException
    {
        // send GetRequest to specified host to retrieve specified object identifier
        
        SNMPVarBindList retrievedVars = new SNMPVarBindList();
        
        
        int errorStatus = 0;
        int errorIndex = 0;
        
        
        SNMPObjectIdentifier requestedObjectIdentifier = new SNMPObjectIdentifier(itemID);
        SNMPVariablePair nextPair = new SNMPVariablePair(requestedObjectIdentifier, new SNMPNull());
        SNMPSequence varList = new SNMPSequence();
        varList.addSNMPObject(nextPair);
        SNMPPDU pdu = new SNMPPDU(SNMPBERCodec.SNMPGETREQUEST, requestID, errorStatus, errorIndex, varList);
        
        SNMPMessage message = new SNMPMessage(version, community, pdu);
        
        byte[] messageEncoding = message.getBEREncoding();
        
        
        /*
        System.out.println("Request Message bytes:");
        
        for (int i = 0; i < messageEncoding.length; ++i)
            System.out.print(hexByte(messageEncoding[i]) + " ");
        */
        
        DatagramPacket outPacket = new DatagramPacket(messageEncoding, messageEncoding.length, hostAddress, remotePort);
        
        dSocket.send(outPacket);
        
        
        while (true)    // wait until receive reply for requestID & OID (or error)
        {
            
            DatagramPacket inPacket = new DatagramPacket(new byte[receiveBufferSize], receiveBufferSize);
            
            dSocket.receive(inPacket);
            
            byte[] encodedMessage = inPacket.getData();
            
            /*
            System.out.println("Message bytes:");
            
            for (int i = 0; i < encodedMessage.length; ++i)
            {
                System.out.print(hexByte(encodedMessage[i]) + " ");
            }
            */
            
            
            SNMPMessage receivedMessage = new SNMPMessage(SNMPBERCodec.extractNextTLV(encodedMessage,0).value);
            SNMPPDU receivedPDU = receivedMessage.getPDU();
            
            // check request identifier; if incorrect, just ignore packet and continue waiting
            if (receivedPDU.getRequestID() == requestID)
            {
                
                // check error status; if retrieval problem, throw SNMPGetException
                if (receivedPDU.getErrorStatus() != 0)
                    throw new SNMPGetException("OID " + itemID + " not available for retrieval", receivedPDU.getErrorIndex(), receivedPDU.getErrorStatus());        
                
                
                varList = receivedPDU.getVarBindList();
                SNMPSequence newPair = (SNMPSequence)(varList.getSNMPObjectAt(0));
                
                SNMPObjectIdentifier newObjectIdentifier = (SNMPObjectIdentifier)(newPair.getSNMPObjectAt(0));
                SNMPObject newValue = newPair.getSNMPObjectAt(1);
                
                // check the object identifier to make sure the correct variable has been received;
                // if not, just continue waiting for receive
                if (newObjectIdentifier.toString().equals(itemID))
                {
                    // got the right one; add it to retrieved var list and break!
                    retrievedVars.addSNMPObject(newPair);
                    break;
                }
            
            }
            
        }
        
        
        requestID++;
        
        
        return retrievedVars;
        
    }
    
    
    
    
    /**
    *    Retrieve the MIB variable values corresponding to the object identifiers
    *    given in the array itemID (in dotted-integer notation). Return as SNMPVarBindList object; 
    *    if no such variable (either due to device not supporting it, or community name having incorrect
    *    access privilege), SNMPGetException thrown
    *    @throws IOException Thrown when timeout experienced while waiting for response to request.
    *    @throws SNMPBadValueException 
    *   @throws SNMPGetException Thrown if one of supplied OIDs has value that can't be retrieved
    */
    
    public SNMPVarBindList getMIBEntry(String[] itemID)
        throws IOException, SNMPBadValueException, SNMPGetException
    {
        // send GetRequest to specified host to retrieve values of specified object identifiers
        
        SNMPVarBindList retrievedVars = new SNMPVarBindList();
        SNMPSequence varList = new SNMPSequence();
        
        int errorStatus = 0;
        int errorIndex = 0;
        
        for (int i = 0; i < itemID.length; i++)
        {
            SNMPObjectIdentifier requestedObjectIdentifier = new SNMPObjectIdentifier(itemID[i]);
            SNMPVariablePair nextPair = new SNMPVariablePair(requestedObjectIdentifier, new SNMPNull());
            varList.addSNMPObject(nextPair);
        }
        
        
        SNMPPDU pdu = new SNMPPDU(SNMPBERCodec.SNMPGETREQUEST, requestID, errorStatus, errorIndex, varList);
        
        SNMPMessage message = new SNMPMessage(version, community, pdu);
        
        byte[] messageEncoding = message.getBEREncoding();
        
        
        /*
        System.out.println("Request Message bytes:");
        
        for (int i = 0; i < messageEncoding.length; ++i)
            System.out.print(hexByte(messageEncoding[i]) + " ");
        */
        
        DatagramPacket outPacket = new DatagramPacket(messageEncoding, messageEncoding.length, hostAddress, remotePort);
        
        
        dSocket.send(outPacket);
        
        
        while (true)    // wait until receive reply for requestID & OID (or error)
        {
            
            DatagramPacket inPacket = new DatagramPacket(new byte[receiveBufferSize], receiveBufferSize);
            
            dSocket.receive(inPacket);
            
            byte[] encodedMessage = inPacket.getData();
            
            /*
            System.out.println("Message bytes:");
            
            for (int i = 0; i < encodedMessage.length; ++i)
            {
                System.out.print(hexByte(encodedMessage[i]) + " ");
            }
            */
            
            
            SNMPMessage receivedMessage = new SNMPMessage(SNMPBERCodec.extractNextTLV(encodedMessage,0).value);
            SNMPPDU receivedPDU = receivedMessage.getPDU();
            
            // check request identifier; if incorrect, just ignore packet and continue waiting
            if (receivedPDU.getRequestID() == requestID)
            {
                
                // check error status; if retrieval problem, throw SNMPGetException
                if (receivedPDU.getErrorStatus() != 0)
                {
                    // determine error index
                    errorIndex = receivedPDU.getErrorIndex();
                    throw new SNMPGetException("OID " + itemID[errorIndex - 1] + " not available for retrieval", errorIndex, receivedPDU.getErrorStatus());        
                }
                
                // copy info from retrieved sequence to var bind list
                varList = receivedPDU.getVarBindList();
                
                for (int i = 0; i < varList.size(); i++)
                {
                    SNMPSequence newPair = (SNMPSequence)(varList.getSNMPObjectAt(i));
                    
                    SNMPObjectIdentifier newObjectIdentifier = (SNMPObjectIdentifier)(newPair.getSNMPObjectAt(0));
                    SNMPObject newValue = newPair.getSNMPObjectAt(1);
                    
                    if (newObjectIdentifier.toString().equals(itemID[i]))
                    {
                        retrievedVars.addSNMPObject(newPair);
                    }
                    else
                    {
                        // wrong OID; throw GetException
                        throw new SNMPGetException("OID " + itemID[i] + " expected at index " + i + ", OID " + newObjectIdentifier + " received", i+1, SNMPRequestException.FAILED);
                    }
                }
                
                break;
            
            }
            
        }
        
        
        requestID++;
        
        
        return retrievedVars;
        
    }
    
    
    
    
    
    /**
    *    Retrieve the MIB variable value corresponding to the object identifier following that
    *    given in itemID (in dotted-integer notation). Return as SNMPVarBindList object; if no
    *    such variable (either due to device not supporting it, or community name having incorrect
    *    access privilege), variable value will be SNMPNull object
    *    @throws IOException Thrown when timeout experienced while waiting for response to request.
    *    @throws SNMPBadValueException 
    *   @throws SNMPGetException Thrown if one the OID following the supplied OID has value that can't be retrieved
    */
    
    public SNMPVarBindList getNextMIBEntry(String itemID)
        throws IOException, SNMPBadValueException, SNMPGetException
    {
        // send GetRequest to specified host to retrieve specified object identifier
        
        SNMPVarBindList retrievedVars = new SNMPVarBindList();
        
        
        int errorStatus = 0;
        int errorIndex = 0;
        
        
        SNMPObjectIdentifier requestedObjectIdentifier = new SNMPObjectIdentifier(itemID);
        SNMPVariablePair nextPair = new SNMPVariablePair(requestedObjectIdentifier, new SNMPNull());
        SNMPSequence varList = new SNMPSequence();
        varList.addSNMPObject(nextPair);
        SNMPPDU pdu = new SNMPPDU(SNMPBERCodec.SNMPGETNEXTREQUEST, requestID, errorStatus, errorIndex, varList);
        
        SNMPMessage message = new SNMPMessage(version, community, pdu);
        
        byte[] messageEncoding = message.getBEREncoding();
        
        
        /*
        System.out.println("Request Message bytes:");
        
        for (int i = 0; i < messageEncoding.length; ++i)
            System.out.print(hexByte(messageEncoding[i]) + " ");
        */
        
        DatagramPacket outPacket = new DatagramPacket(messageEncoding, messageEncoding.length, hostAddress, remotePort);
        
        
        dSocket.send(outPacket);
        
        
        while (true)    // wait until receive reply for requestID & OID (or error)
        {
            
            DatagramPacket inPacket = new DatagramPacket(new byte[receiveBufferSize], receiveBufferSize);
            
            dSocket.receive(inPacket);
            
            byte[] encodedMessage = inPacket.getData();
            
            /*
            System.out.println("Message bytes:");
            
            for (int i = 0; i < encodedMessage.length; ++i)
            {
                System.out.print(hexByte(encodedMessage[i]) + " ");
            }
            */
            
            
            SNMPMessage receivedMessage = new SNMPMessage(SNMPBERCodec.extractNextTLV(encodedMessage,0).value);
            SNMPPDU receivedPDU = receivedMessage.getPDU();
            
            // check request identifier; if incorrect, just ignore packet and continue waiting
            if (receivedPDU.getRequestID() == requestID)
            {
                
                // check error status; if retrieval problem, throw SNMPGetException
                if (receivedPDU.getErrorStatus() != 0)
                    throw new SNMPGetException("OID " + itemID + " not available for retrieval", receivedPDU.getErrorIndex(), receivedPDU.getErrorStatus());        
                
                
                varList = receivedPDU.getVarBindList();
                SNMPSequence newPair = (SNMPSequence)(varList.getSNMPObjectAt(0));
                
                SNMPObjectIdentifier newObjectIdentifier = (SNMPObjectIdentifier)(newPair.getSNMPObjectAt(0));
                SNMPObject newValue = newPair.getSNMPObjectAt(1);
                
                retrievedVars.addSNMPObject(newPair);
                
                break;
            
            }
            
        }
        
        
        requestID++;
        
        
        return retrievedVars;
        
    }
    
    
    
    
    
    /**
    *    Retrieve the MIB variable value corresponding to the object identifiers following those
    *    given in the itemID array (in dotted-integer notation). Return as SNMPVarBindList object; 
    *    if no such variable (either due to device not supporting it, or community name having 
    *    incorrect access privilege), SNMPGetException thrown
    *    @throws IOException Thrown when timeout experienced while waiting for response to request.
    *    @throws SNMPBadValueException 
    *   @throws SNMPGetException Thrown if OID following one of supplied OIDs has value that can't be retrieved
    */
    
    public SNMPVarBindList getNextMIBEntry(String[] itemID)
        throws IOException, SNMPBadValueException, SNMPGetException
    {
        // send GetRequest to specified host to retrieve values of specified object identifiers
        
        SNMPVarBindList retrievedVars = new SNMPVarBindList();
        SNMPSequence varList = new SNMPSequence();
        
        int errorStatus = 0;
        int errorIndex = 0;
        
        for (int i = 0; i < itemID.length; i++)
        {
            SNMPObjectIdentifier requestedObjectIdentifier = new SNMPObjectIdentifier(itemID[i]);
            SNMPVariablePair nextPair = new SNMPVariablePair(requestedObjectIdentifier, new SNMPNull());
            varList.addSNMPObject(nextPair);
        }
        
        SNMPPDU pdu = new SNMPPDU(SNMPBERCodec.SNMPGETNEXTREQUEST, requestID, errorStatus, errorIndex, varList);
        SNMPMessage message = new SNMPMessage(version, community, pdu);
        
        byte[] messageEncoding = message.getBEREncoding();
        
        
        /*
        System.out.println("Request Message bytes:");
        
        for (int i = 0; i < messageEncoding.length; ++i)
            System.out.print(hexByte(messageEncoding[i]) + " ");
        */
        
        DatagramPacket outPacket = new DatagramPacket(messageEncoding, messageEncoding.length, hostAddress, remotePort);
        
        
        dSocket.send(outPacket);
        
        
        while (true)    // wait until receive reply for requestID & OID (or error)
        {
            
            DatagramPacket inPacket = new DatagramPacket(new byte[receiveBufferSize], receiveBufferSize);
            
            dSocket.receive(inPacket);
            
            byte[] encodedMessage = inPacket.getData();
            
            /*
            System.out.println("Message bytes:");
            
            for (int i = 0; i < encodedMessage.length; ++i)
            {
                System.out.print(hexByte(encodedMessage[i]) + " ");
            }
            */
            
            
            SNMPMessage receivedMessage = new SNMPMessage(SNMPBERCodec.extractNextTLV(encodedMessage,0).value);
            SNMPPDU receivedPDU = receivedMessage.getPDU();
            
            // check request identifier; if incorrect, just ignore packet and continue waiting
            if (receivedPDU.getRequestID() == requestID)
            {
                
                // check error status; if retrieval problem, throw SNMPGetException
                if (receivedPDU.getErrorStatus() != 0)
                {
                    // determine error index
                    errorIndex = receivedPDU.getErrorIndex();
                    throw new SNMPGetException("OID following " + itemID[errorIndex - 1] + " not available for retrieval", errorIndex, receivedPDU.getErrorStatus());        
                }
                
                // copy info from retrieved sequence to var bind list
                varList = receivedPDU.getVarBindList();
                
                for (int i = 0; i < varList.size(); i++)
                {
                    SNMPSequence newPair = (SNMPSequence)(varList.getSNMPObjectAt(i));
                    
                    SNMPObjectIdentifier newObjectIdentifier = (SNMPObjectIdentifier)(newPair.getSNMPObjectAt(0));
                    SNMPObject newValue = newPair.getSNMPObjectAt(1);
                    
                    retrievedVars.addSNMPObject(newPair);
                    
                }
                
                break;
            
            }
            
        }
        
        
        requestID++;
        
        
        return retrievedVars;
        
    }
    
    
    
    
    
    
    
    /**
    *    Set the MIB variable value of the object identifier
    *    given in startID (in dotted-integer notation). Return SNMPVarBindList object returned
    *    by device in its response; can be used to check that setting was successful.
    *    Uses SNMPGetNextRequests to retrieve variable values in sequence.
    *    @throws IOException Thrown when timeout experienced while waiting for response to request.
    *    @throws SNMPBadValueException 
    */
    
    public SNMPVarBindList setMIBEntry(String itemID, SNMPObject newValue)
        throws IOException, SNMPBadValueException, SNMPSetException
    {
        // send SetRequest to specified host to set value of specified object identifier
        
        SNMPVarBindList retrievedVars = new SNMPVarBindList();
        
        int errorStatus = 0;
        int errorIndex = 0;
        
        SNMPObjectIdentifier requestedObjectIdentifier = new SNMPObjectIdentifier(itemID);
        SNMPVariablePair nextPair = new SNMPVariablePair(requestedObjectIdentifier, newValue);
        
            
        
        SNMPSequence varList = new SNMPSequence();
        varList.addSNMPObject(nextPair);
        SNMPPDU pdu = new SNMPPDU(SNMPBERCodec.SNMPSETREQUEST, requestID, errorStatus, errorIndex, varList);
        
        
        SNMPMessage message = new SNMPMessage(version, community, pdu);
        byte[] messageEncoding = message.getBEREncoding();
        
        /*
        System.out.println("Message bytes:");
        
        for (int i = 0; i < messageEncoding.length; ++i)
        {
            System.out.print(hexByte(messageEncoding[i]) + " ");
        }
        */
        
        
        DatagramPacket outPacket = new DatagramPacket(messageEncoding, messageEncoding.length, hostAddress, remotePort);
        
        
        dSocket.send(outPacket);
        
        
        while (true)    // wait until receive reply for correct OID (or error)
        {
            
            DatagramPacket inPacket = new DatagramPacket(new byte[receiveBufferSize], receiveBufferSize);
            
            dSocket.receive(inPacket);
            
            
            byte[] encodedMessage = inPacket.getData();
            
            /*
            System.out.println("Message bytes:");
            
            for (int i = 0; i < encodedMessage.length; ++i)
            {
                System.out.print((encodedMessage[i]) + " ");
            }
            */
        
        
            
            SNMPMessage receivedMessage = new SNMPMessage(SNMPBERCodec.extractNextTLV(encodedMessage,0).value);
            
            SNMPPDU receivedPDU = receivedMessage.getPDU();
            
            
            // check request identifier; if incorrect, just ignore packet and continue waiting
            if (receivedPDU.getRequestID() == requestID)
            {
                
                // check error status; if retrieval problem, throw SNMPGetException
                if (receivedPDU.getErrorStatus() != 0)
                {
                    switch (receivedPDU.getErrorStatus())
                    {
                        case 1:
                            throw new SNMPSetException("Value supplied for OID " + itemID + " too big.", receivedPDU.getErrorIndex(), receivedPDU.getErrorStatus());
                        
                        case 2:
                            throw new SNMPSetException("OID " + itemID + " not available for setting.", receivedPDU.getErrorIndex(), receivedPDU.getErrorStatus());
                        
                        case 3:
                            throw new SNMPSetException("Bad value supplied for OID " + itemID + ".", receivedPDU.getErrorIndex(), receivedPDU.getErrorStatus());
                            
                        case 4:
                            throw new SNMPSetException("OID " + itemID + " read-only.", receivedPDU.getErrorIndex(), receivedPDU.getErrorStatus());
                            
                        default:
                            throw new SNMPSetException("Error setting OID " + itemID + ".", receivedPDU.getErrorIndex(), receivedPDU.getErrorStatus());    
                            
                    }
                }
                
                
                varList = receivedPDU.getVarBindList();
                SNMPSequence newPair = (SNMPSequence)(varList.getSNMPObjectAt(0));
                
                // check the object identifier to make sure the correct variable has been received;
                // if not, just continue waiting for receive
                if (((SNMPObjectIdentifier)newPair.getSNMPObjectAt(0)).toString().equals(itemID))
                {
                    // got the right one; add it to retrieved var list and break!
                    retrievedVars.addSNMPObject(newPair);
                    break;
                }
            
            }
            
        }
        
        
        requestID++;
    
        
        return retrievedVars;
        
    }
    
    
    
    
    /**
    *    Set the MIB variable values of the supplied object identifiers given in the 
    *    itemID array (in dotted-integer notation). Return SNMPVarBindList returned
    *    by device in its response; can be used to check that setting was successful.
    *    Uses SNMPGetNextRequests to retrieve variable values in sequence.
    *    @throws IOException Thrown when timeout experienced while waiting for response to request.
    *    @throws SNMPBadValueException 
    */
    
    public SNMPVarBindList setMIBEntry(String[] itemID, SNMPObject[] newValue)
        throws IOException, SNMPBadValueException, SNMPSetException
    {
        // check that OID and value arrays have same size
        if (itemID.length != newValue.length)
        {
            throw new SNMPSetException("OID and value arrays must have same size", 0, SNMPRequestException.FAILED);
        }
        
        
        // send SetRequest to specified host to set values of specified object identifiers
        
        SNMPVarBindList retrievedVars = new SNMPVarBindList();
        SNMPSequence varList = new SNMPSequence();
        
        int errorStatus = 0;
        int errorIndex = 0;
        
        
        for (int i = 0; i < itemID.length; i++)
        {
            SNMPObjectIdentifier requestedObjectIdentifier = new SNMPObjectIdentifier(itemID[i]);
            SNMPVariablePair nextPair = new SNMPVariablePair(requestedObjectIdentifier, newValue[i]);
            varList.addSNMPObject(nextPair);
        }
        
        SNMPPDU pdu = new SNMPPDU(SNMPBERCodec.SNMPSETREQUEST, requestID, errorStatus, errorIndex, varList);
        SNMPMessage message = new SNMPMessage(version, community, pdu);
        
        byte[] messageEncoding = message.getBEREncoding();
        
        /*
        System.out.println("Message bytes:");
        
        for (int i = 0; i < messageEncoding.length; ++i)
        {
            System.out.print(getHex(messageEncoding[i]) + " ");
        }
        */
        
        
        DatagramPacket outPacket = new DatagramPacket(messageEncoding, messageEncoding.length, hostAddress, remotePort);
        
        dSocket.send(outPacket);
        
        
        while (true)    // wait until receive reply for correct OID (or error)
        {
            
            DatagramPacket inPacket = new DatagramPacket(new byte[receiveBufferSize], receiveBufferSize);
            
            dSocket.receive(inPacket);
            
            byte[] encodedMessage = inPacket.getData();
            
            /*
            System.out.println("Message bytes:");
            
            for (int i = 0; i < encodedMessage.length; ++i)
            {
                System.out.print((encodedMessage[i]) + " ");
            }
            */
        
        
            
            SNMPMessage receivedMessage = new SNMPMessage(SNMPBERCodec.extractNextTLV(encodedMessage,0).value);
            
            SNMPPDU receivedPDU = receivedMessage.getPDU();
            
            
            // check request identifier; if incorrect, just ignore packet and continue waiting
            if (receivedPDU.getRequestID() == requestID)
            {
                
                // check error status; if retrieval problem, throw SNMPGetException
                if (receivedPDU.getErrorStatus() != 0)
                {
                    errorIndex = receivedPDU.getErrorIndex();
                    
                    switch (receivedPDU.getErrorStatus())
                    {
                        case 1:
                            throw new SNMPSetException("Value supplied for OID " + itemID[errorIndex - 1] + " too big.", receivedPDU.getErrorIndex(), receivedPDU.getErrorStatus());
                        
                        case 2:
                            throw new SNMPSetException("OID " + itemID[errorIndex - 1] + " not available for setting.", receivedPDU.getErrorIndex(), receivedPDU.getErrorStatus());
                        
                        case 3:
                            throw new SNMPSetException("Bad value supplied for OID " + itemID[errorIndex - 1] + ".", receivedPDU.getErrorIndex(), receivedPDU.getErrorStatus());
                            
                        case 4:
                            throw new SNMPSetException("OID " + itemID[errorIndex - 1] + " read-only.", receivedPDU.getErrorIndex(), receivedPDU.getErrorStatus());
                            
                        default:
                            throw new SNMPSetException("Error setting OID " + itemID[errorIndex - 1] + ".", receivedPDU.getErrorIndex(), receivedPDU.getErrorStatus());
                            
                    }
                }
                
                
                // copy info from retrieved sequence to var bind list
                varList = receivedPDU.getVarBindList();
                
                for (int i = 0; i < varList.size(); i++)
                {
                    SNMPSequence newPair = (SNMPSequence)(varList.getSNMPObjectAt(i));
                    
                    SNMPObjectIdentifier newObjectIdentifier = (SNMPObjectIdentifier)(newPair.getSNMPObjectAt(0));
                    //SNMPObject receivedValue = newPair.getSNMPObjectAt(1);
                    
                    if (newObjectIdentifier.toString().equals(itemID[i]))
                    {
                        retrievedVars.addSNMPObject(newPair);
                    }
                    else
                    {
                        // wrong OID; throw GetException
                        throw new SNMPSetException("OID " + itemID[i] + " expected at index " + i + ", OID " + newObjectIdentifier + " received", i+1, SNMPRequestException.FAILED);
                    }
                }
                
                break;
            
            }
            
        }
        
        
        requestID++;
    
        
        return retrievedVars;
        
    }
    
    
    
    
    /**
    *    Retrieve all MIB variable values whose OIDs start with the supplied baseID. Since the entries of
    *   an SNMP table have the form  <baseID>.<tableEntry>.<index>, this will retrieve all of the table 
    *   data as an SNMPVarBindList object consisting of sequence of SNMPVariablePairs.
    *    Uses SNMPGetNextRequests to retrieve variable values in sequence.
    *    @throws IOException Thrown when timeout experienced while waiting for response to request.
    *    @throws SNMPBadValueException 
    */
    
    public SNMPVarBindList retrieveMIBTable(String baseID)
        throws IOException, SNMPBadValueException, SNMPGetException
    {
        // send GetNextRequests until receive
        // an error message or a repeat of the object identifier we sent out
        SNMPVarBindList retrievedVars = new SNMPVarBindList();
        
        
        int errorStatus = 0;
        int errorIndex = 0;
        
        String currentID = baseID;
        SNMPObjectIdentifier requestedObjectIdentifier = new SNMPObjectIdentifier(currentID);
        
        
        while (errorStatus == 0)
        {
            
            SNMPVariablePair nextPair = new SNMPVariablePair(requestedObjectIdentifier, new SNMPNull());
            SNMPSequence varList = new SNMPSequence();
            varList.addSNMPObject(nextPair);
            SNMPPDU pdu = new SNMPPDU(SNMPBERCodec.SNMPGETNEXTREQUEST, requestID, errorStatus, errorIndex, varList);
            SNMPMessage message = new SNMPMessage(version, community, pdu);
            byte[] messageEncoding = message.getBEREncoding();
            DatagramPacket outPacket = new DatagramPacket(messageEncoding, messageEncoding.length, hostAddress, remotePort);
            
            /*
            System.out.println("Request bytes:");
            
            for (int i = 0; i < messageEncoding.length; ++i)
            {
                System.out.print(getHex(messageEncoding[i]) + " ");
            }
            */
            
            dSocket.send(outPacket);
            
            
            DatagramPacket inPacket = new DatagramPacket(new byte[receiveBufferSize], receiveBufferSize);
        
            dSocket.receive(inPacket);
            
            byte[] encodedMessage = inPacket.getData();
            
            
            SNMPMessage receivedMessage = new SNMPMessage(SNMPBERCodec.extractNextTLV(encodedMessage,0).value);
            SNMPPDU receivedPDU = receivedMessage.getPDU();
            
            // check request identifier; if incorrect, just ignore packet and continue waiting
            if (receivedPDU.getRequestID() == requestID)
            {
                
                // check error status; if retrieval problem, just break - could be there are no additional OIDs
                if (receivedPDU.getErrorStatus() != 0)
                {
                    break;
                    //throw new SNMPGetException("OID following " + requestedObjectIdentifier + " not available for retrieval");        
                }
                
                varList = receivedPDU.getVarBindList();
                SNMPSequence newPair = (SNMPSequence)(varList.getSNMPObjectAt(0));
                
                SNMPObjectIdentifier newObjectIdentifier = (SNMPObjectIdentifier)(newPair.getSNMPObjectAt(0));
                SNMPObject newValue = newPair.getSNMPObjectAt(1);
                
                // now see if retrieved ID starts with table base; if not, done with table - break
                String newOIDString = (String)newObjectIdentifier.toString();
                if (!newOIDString.startsWith(baseID))
                    break;
                
                retrievedVars.addSNMPObject(newPair);
                    
                requestedObjectIdentifier = newObjectIdentifier;
            
                requestID++;
            
            }
            
            
        }
            
        
        return retrievedVars;
        
    }
    
    
    
    
    /**
    *    Retrieve all MIB variable values whose OIDs start with the supplied baseIDs. The normal way for
    *   this to be used is for the base OID array to consist of the base OIDs of the columns of a table.
    *   This method will then retrieve all of the entries of the table corresponding to these columns, one 
    *   row at a time (i.e., the entries for each row will be retrieved in a single SNMP request). This 
    *   will retrieve the table data as an SNMPVarBindList object consisting of sequence of SNMPVariablePairs,
    *   with the entries for each row grouped together. This may provide a more convenient arrangement of
    *   the table data than the simpler retrieveMIBTable method taking a single OID as argument; in addition,
    *   it's more efficient, requiring one SNMP request per row rather than one request per entry.
    *    Uses SNMPGetNextRequests to retrieve variable values for each row in sequence.
    *    @throws IOException Thrown when timeout experienced while waiting for response to request.
    *    @throws SNMPBadValueException 
    *   @throws SNMPGetException Thrown if incomplete row retrieved
    */
    
    public SNMPVarBindList retrieveMIBTable(String[] baseID)
        throws IOException, SNMPBadValueException, SNMPGetException
    {
        // send GetNextRequests until receive
        // an error message or a repeat of the object identifier we sent out
        SNMPVarBindList retrievedVars = new SNMPVarBindList();
        
        int errorStatus = 0;
        int errorIndex = 0;
        
        SNMPObjectIdentifier[] requestedObjectIdentifier = new SNMPObjectIdentifier[baseID.length];
        for (int i = 0; i < baseID.length; i++)
        {
               requestedObjectIdentifier[i] = new SNMPObjectIdentifier(baseID[i]);
        }
        

retrievalLoop:
        
        while (errorStatus == 0)
        {
            
            SNMPSequence varList = new SNMPSequence();
            
            for (int i = 0; i < requestedObjectIdentifier.length; i++)
            {
                SNMPVariablePair nextPair = new SNMPVariablePair(requestedObjectIdentifier[i], new SNMPNull());
                varList.addSNMPObject(nextPair);
            }
            
            SNMPPDU pdu = new SNMPPDU(SNMPBERCodec.SNMPGETNEXTREQUEST, requestID, errorStatus, errorIndex, varList);
            SNMPMessage message = new SNMPMessage(version, community, pdu);
            
            byte[] messageEncoding = message.getBEREncoding();
            
            DatagramPacket outPacket = new DatagramPacket(messageEncoding, messageEncoding.length, hostAddress, remotePort);
            
            /*
            System.out.println("Request bytes:");
            
            for (int i = 0; i < messageEncoding.length; ++i)
            {
                System.out.print(getHex(messageEncoding[i]) + " ");
            }
            */
            
            dSocket.send(outPacket);
            
            
            DatagramPacket inPacket = new DatagramPacket(new byte[receiveBufferSize], receiveBufferSize);
        
            dSocket.receive(inPacket);
            
            byte[] encodedMessage = inPacket.getData();
            
            
            SNMPMessage receivedMessage = new SNMPMessage(SNMPBERCodec.extractNextTLV(encodedMessage,0).value);
            SNMPPDU receivedPDU = receivedMessage.getPDU();
            
            // check request identifier; if incorrect, just ignore packet and continue waiting
            if (receivedPDU.getRequestID() == requestID)
            {
                
                // check error status; if retrieval problem for error index 1, just break - assume there are no additional OIDs
                // to retrieve. If index is other than 1, throw exception
                if (receivedPDU.getErrorStatus() != 0)
                {
                    int retrievedErrorIndex = receivedPDU.getErrorIndex();
                    
                    if (retrievedErrorIndex == 1)
                    {
                        break retrievalLoop;
                    }
                    else
                    {
                        throw new SNMPGetException("OID following " + requestedObjectIdentifier[retrievedErrorIndex - 1] + " not available for retrieval", retrievedErrorIndex, receivedPDU.getErrorStatus());
                    }    
                }
                
                // copy info from retrieved sequence to var bind list
                varList = receivedPDU.getVarBindList();
                
                // make sure got the right number of vars in reply; if not, throw GetException
                if(varList.size() != requestedObjectIdentifier.length)
                {
                    throw new SNMPGetException("Incomplete row of table received", 0, SNMPRequestException.FAILED);
                }
                
                // copy the retrieved variable pairs into retrievedVars
                for (int i = 0; i < varList.size(); i++)
                {
                    SNMPSequence newPair = (SNMPSequence)(varList.getSNMPObjectAt(i));
                    
                    SNMPObjectIdentifier newObjectIdentifier = (SNMPObjectIdentifier)(newPair.getSNMPObjectAt(0));
                    SNMPObject newValue = newPair.getSNMPObjectAt(1);
                    
                    // now see if retrieved ID starts with table base; if not, done with table - break
                    String newOIDString = (String)newObjectIdentifier.toString();
                    if (!newOIDString.startsWith(baseID[i]))
                    {
                        if (i == 0)
                        {
                            // it's the first element of the row; just break
                            break retrievalLoop;
                        }
                        else
                        {
                            // it's a subsequent row element; throw exception
                            throw new SNMPGetException("Incomplete row of table received", i+1, SNMPRequestException.FAILED);
                        }
                    }
                        
                    retrievedVars.addSNMPObject(newPair);
                    
                    // set requested identifiers array to current identifiers to do get-next for next row
                    requestedObjectIdentifier[i] = newObjectIdentifier;
                }
                
                
                requestID++;
            
            }
            
            
        }
            
        
        return retrievedVars;
        
    }
    
    
    
    
    
    
    /**
    *   Set the size of the buffer used to receive response packets. RFC 1157 stipulates that an SNMP
    *   implementation must be able to receive packets of at least 484 bytes, so if you try to set the
    *   size to a value less than this, the receive buffer size will be set to 484 bytes. In addition,
    *   the maximum size of a UDP packet payload is 65535 bytes, so setting the buffer to a larger size
    *   will just waste memory. The default value is 512 bytes. The value may need to be increased if
    *   get-requests are issued for multiple OIDs.
    */
    
    public void setReceiveBufferSize(int receiveBufferSize)
    {
        if (receiveBufferSize >= 484)
        {
            this.receiveBufferSize = receiveBufferSize;
        }
        else
        {
            this.receiveBufferSize = 484;
        }
    }
    
    
    
    /**
    *   Returns the current size of the buffer used to receive response packets. 
    */
    
    public int getReceiveBufferSize()
    {
        return this.receiveBufferSize;
    }
    
    
    
}
