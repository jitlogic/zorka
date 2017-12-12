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
import java.util.*;


/**
*    The class SNMPTrapListenerInterface implements a server which listens for trap and inform request 
* 	 messages sent from remote SNMP entities. The approach is that from version 1 and 2c of SNMP, using no 
* 	 encryption of data. Communication occurs via UDP, using port 162, the standard SNMP trap port, or an
*    alternate (non-standard) port supplied in the constructor. This interface can handle both SNMPv1 and 
*    SNMPv2 traps (which have different PDU types), and SNMPv2 Inform Requests.
*
*    Applications utilize this class with classes which implement the SNMPTrapListener or SNMPv2TrapListener or
*    SNMPv2InformRequestListener interfaces. These must provide a processTrap(), processv2Trap() or processInformRequest() 
* 	 method, and are registered/unregistered with this class through its addv1TrapListener()/removev1TrapListener(),
*    addv2TrapListener()/removev2TrapListener(), or addv2InformRequestListener()/removev2InformRequestListener()
*    methods.
*/

public class SNMPTrapReceiverInterface
                        implements Runnable
{
    public static final int SNMP_TRAP_PORT = 162;
    
    // largest size for datagram packet payload; based on
    // RFC 1157, need to handle messages of at least 484 bytes
    private int receiveBufferSize = 512;
    
    private DatagramSocket dSocket;
    private Thread receiveThread;
    
    private Vector v1TrapListenerVector;
    private Vector v2TrapListenerVector;
    private Vector v2InformRequestListenerVector;
    private PrintWriter errorLog;
    
    
    
    
    /**
    *    Construct a new trap receiver object to receive traps from remote SNMP hosts.
    *    Uses the standard SNMP trap reception port 162, and System.out to report errors.
    */
    
    public SNMPTrapReceiverInterface()
        throws SocketException
    {
        // set System.out as the error writer
        this(new PrintWriter(System.out));
    }
    
    
    
    /**
    *    Construct a new trap receiver object to receive traps from remote SNMP hosts.
    *    Uses the specified Writer to deliver error messages, and the standard SNMP trap
    *    port 162.
    */
    
    public SNMPTrapReceiverInterface(PrintWriter errorReceiver)
        throws SocketException
    {
        this(errorReceiver, SNMP_TRAP_PORT);
    }
    
   
    
    /**
     *    Construct a new trap receiver object to receive traps from remote SNMP hosts.
     *    Uses the specified port for trap reception, and System.out to report errors.
     */
     
     public SNMPTrapReceiverInterface(int trapReceivePort)
         throws SocketException
     {
         this(new PrintWriter(System.out), trapReceivePort);
     }
     
     
     
     /**
     *    Construct a new trap receiver object to receive traps from remote SNMP hosts.
     *    This version will accept messages from all hosts using any community name. Uses the
     *   specified Writer to deliver error messages and the specified port to listen on.
     */
     
     public SNMPTrapReceiverInterface(PrintWriter errorReceiver, int trapReceivePort)
         throws SocketException
     {
         dSocket = new DatagramSocket(trapReceivePort);
         
         v1TrapListenerVector = new Vector();
         v2TrapListenerVector = new Vector();
         v2InformRequestListenerVector = new Vector();
         
         receiveThread = new Thread(this);
         
         errorLog = errorReceiver;
         
     }
    
    
    
    /**
    *    Set the specified PrintWriter to receive error messages.
    */
    
    public void setErrorReceiver(PrintWriter errorReceiver)
    {
        errorLog = errorReceiver;
    }
    
    
    
    public void addv1TrapListener(SNMPv1TrapListener listener)
    {
        // see if listener already added; if so, ignore
        for (int i = 0; i < v1TrapListenerVector.size(); i++)
        {
            if (listener == v1TrapListenerVector.elementAt(i))
            {
                return;
            }
        }
        
        // if got here, it's not in the list; add it
        v1TrapListenerVector.add(listener);
    }
    
    
    
    public void removev1TrapListener(SNMPv1TrapListener listener)
    {
        // see if listener in list; if so, remove, if not, ignore
        for (int i = 0; i < v1TrapListenerVector.size(); i++)
        {
            if (listener == v1TrapListenerVector.elementAt(i))
            {
                v1TrapListenerVector.removeElementAt(i);
                break;
            }
        }
        
    }
    
    
    
    public void addv2TrapListener(SNMPv2TrapListener listener)
    {
        // see if listener already added; if so, ignore
        for (int i = 0; i < v2TrapListenerVector.size(); i++)
        {
            if (listener == v2TrapListenerVector.elementAt(i))
            {
                return;
            }
        }
        
        // if got here, it's not in the list; add it
        v2TrapListenerVector.add(listener);
    }
    
    
    
    public void removev2TrapListener(SNMPv2TrapListener listener)
    {
        // see if listener in list; if so, remove, if not, ignore
        for (int i = 0; i < v2TrapListenerVector.size(); i++)
        {
            if (listener == v2TrapListenerVector.elementAt(i))
            {
                v2TrapListenerVector.removeElementAt(i);
                break;
            }
        }
        
    }
    
    
    
    public void addv2InformRequestListener(SNMPv2InformRequestListener listener)
    {
        // see if listener already added; if so, ignore
        for (int i = 0; i < v2InformRequestListenerVector.size(); i++)
        {
            if (listener == v2InformRequestListenerVector.elementAt(i))
            {
                return;
            }
        }
        
        // if got here, it's not in the list; add it
        v2InformRequestListenerVector.add(listener);
    }
    
    
    
    public void removev2InformRequestListener(SNMPv2InformRequestListener listener)
    {
        // see if listener in list; if so, remove, if not, ignore
        for (int i = 0; i < v2InformRequestListenerVector.size(); i++)
        {
            if (listener == v2InformRequestListenerVector.elementAt(i))
            {
                v2InformRequestListenerVector.removeElementAt(i);
                break;
            }
        }
        
    }

    
    
    /**
    *    Start listening for trap and inform messages.
    */
    
    public void startReceiving()
    {
        // if receiveThread not already running, start it
        if (!receiveThread.isAlive())
        {
            receiveThread = new Thread(this);
            receiveThread.start();
        }
    }
    

    
    
    /**
    *    Stop listening for trap and inform messages.
    */
    
    public void stopReceiving()
        throws SocketException
    {
        // interrupt receive thread so it will die a natural death
        receiveThread.interrupt();
    }

    
    
    
    
    /**
    *    The run() method for the trap interface's listener. Just waits for trap or inform messages to
    *    come in on port 162, then dispatches the recieved PDUs to each of the registered 
    *    listeners by calling their processTrap() or processInform() methods.
    */
    
    public void run()
    {
        
        int errorStatus = 0;
        int errorIndex = 0;
        
        
        
        while (!receiveThread.isInterrupted())
        {
            
            try
               {
                
                DatagramPacket inPacket = new DatagramPacket(new byte[receiveBufferSize], receiveBufferSize);
        
                dSocket.receive(inPacket);
                
                byte[] encodedMessage = inPacket.getData();
                
                // get IP address of sender, to supply with SNMPv2 traps 
                // which don't include this in the PDU
                InetAddress agentIPAddress = inPacket.getAddress();
                
                
                /*
                errorLog.println("Message bytes length (in): " + inPacket.getLength());
                
                errorLog.println("Message bytes (in):");
                for (int i = 0; i < encodedMessage.length; ++i)
                {
                    errorLog.print(hexByte(encodedMessage[i]) + " ");
                }
                errorLog.println("\n");
                */
                
                
                SNMPMessage receivedMessage = new SNMPMessage(SNMPBERCodec.extractNextTLV(encodedMessage,0).value);
                
                String communityName = receivedMessage.getCommunityName();
                
                Object receivedPDU = receivedMessage.getPDUAsObject();
                
                if (!(receivedPDU instanceof SNMPv1TrapPDU) && !(receivedPDU instanceof SNMPv2TrapPDU) && !(receivedPDU instanceof SNMPv2InformRequestPDU))
                {
                    throw new SNMPBadValueException("PDU received that's not a v1 or v2 trap or inform request; message payload of type " + receivedPDU.getClass().toString());
                }
                
                // pass the received trap PDU to the processTrap or procesv2Trap method of any listeners
                if (receivedPDU instanceof SNMPv1TrapPDU)
                {
                    for (int i = 0; i < v1TrapListenerVector.size(); i++)
                    {
                        SNMPv1TrapListener listener = (SNMPv1TrapListener)v1TrapListenerVector.elementAt(i);
                        
                        listener.processv1Trap((SNMPv1TrapPDU)receivedPDU, communityName);
                    }
                }
                else if (receivedPDU instanceof SNMPv2TrapPDU)
                {
                    for (int i = 0; i < v2TrapListenerVector.size(); i++)
                    {
                        SNMPv2TrapListener listener = (SNMPv2TrapListener)v2TrapListenerVector.elementAt(i);
                        
                        listener.processv2Trap((SNMPv2TrapPDU)receivedPDU, communityName, agentIPAddress);
                    }
                }
                else if (receivedPDU instanceof SNMPv2InformRequestPDU)
                {
                    for (int i = 0; i < v2InformRequestListenerVector.size(); i++)
                    {
                        SNMPv2InformRequestListener listener = (SNMPv2InformRequestListener)v2InformRequestListenerVector.elementAt(i);
                        
                        listener.processv2InformRequest((SNMPv2InformRequestPDU)receivedPDU, communityName, agentIPAddress);
                    }
                }
                
        
            }
            catch (IOException e)
            {
                // just report the problem
                errorLog.println("IOException during request processing: " + e.toString());
                errorLog.flush();
            }
            catch (SNMPBadValueException e)
            {
                // just report the problem
                errorLog.println("SNMPBadValueException during request processing: " + e.toString());
                errorLog.flush();
            }
            catch (Exception e)
            {
                // just report the problem
                errorLog.println("Exception during request processing: " + e.toString());
                errorLog.flush();
            }
            
        }
                
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
    *   Returns the current size of the buffer used to receive packets. 
    */
    
    public int getReceiveBufferSize()
    {
        return this.receiveBufferSize;
    }
    
    
    
}