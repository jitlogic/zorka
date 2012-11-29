/*
 * SNMP Trap Test
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

package com.jitlogic.zorka.agent.manualtest;


import java.net.*;
import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.event.*;
import java.io.*;
import com.jitlogic.zorka.libsnmp.*;




public class SNMPTrapSendTest extends JFrame
                            implements ActionListener
{
    
    JButton sendv1TrapButton, sendv2TrapButton, sendv2InformRequestButton;
    JButton clearButton;
    JTextArea messagesArea;
    JScrollPane messagesScroll;
    JTextField hostIDField, communityField, OIDField, valueField, enterpriseField, agentField;
    JLabel authorLabel, hostIDLabel, communityLabel, OIDLabel, valueLabel, enterpriseLabel, agentLabel, genericTrapLabel, specificTrapLabel;
    JComboBox valueTypeBox, genericTrapBox, specificTrapBox;
    
    MenuBar theMenubar;
    Menu fileMenu;
    MenuItem aboutItem, quitItem;
    
    
    SNMPTrapSenderInterface trapSenderInterface;
    SNMPInformRequestSenderInterface informRequestSenderInterface;
    
    PipedReader errorReader;
    PipedWriter errorWriter;
    
    
    
    
    // WindowCloseAdapter to catch window close-box closings
    private class WindowCloseAdapter extends WindowAdapter
    { 
        public void windowClosing(WindowEvent e)
        {
            System.exit(0);
        }
    };
            
    
    public SNMPTrapSendTest() 
    {
        setUpDisplay();
            
        try
        {
            errorReader = new PipedReader();
            errorWriter = new PipedWriter(errorReader);
            
            trapSenderInterface = new SNMPTrapSenderInterface();
            informRequestSenderInterface = new SNMPInformRequestSenderInterface();
            
        }
        catch(Exception e)
        {
            messagesArea.append("Problem starting Trap Test: " + e.toString() + "\n");
        }
    }
    
    
    
    private void setUpDisplay()
    {
        
        this.setTitle("SNMP Trap Send Test");
            
        this.getRootPane().setBorder(new BevelBorder(BevelBorder.RAISED));
        
        // set fonts to smaller-than-normal size, for compaction!
        /*
        FontUIResource appFont = new FontUIResource("SansSerif", Font.PLAIN, 10);
        UIDefaults defaults = UIManager.getLookAndFeelDefaults();
        Enumeration keys = defaults.keys();
        
        while (keys.hasMoreElements())
        {
            String nextKey = (String)(keys.nextElement());
            if ((nextKey.indexOf("font") > -1) || (nextKey.indexOf("Font") > -1))
            {
                UIManager.put(nextKey, appFont);
            }
        }
        */
        
        
        // add WindowCloseAdapter to catch window close-box closings
        addWindowListener(new WindowCloseAdapter());

        
        theMenubar = new MenuBar();
        this.setMenuBar(theMenubar);
        fileMenu = new Menu("File");
        
        aboutItem = new MenuItem("About...");
        aboutItem.setActionCommand("about");
        aboutItem.addActionListener(this);
        fileMenu.add(aboutItem);
        
        fileMenu.addSeparator();
        
        quitItem = new MenuItem("Quit");
        quitItem.setActionCommand("quit");
        quitItem.addActionListener(this);
        fileMenu.add(quitItem);
        
        theMenubar.add(fileMenu);
        
        
        hostIDLabel = new JLabel("Trap receiver address:");
        hostIDField = new JTextField(20);
        hostIDField.setText("10.0.1.1");
        hostIDField.setEditable(true);
        
        OIDLabel = new JLabel("additional variable OID:");
        OIDField = new JTextField(20);
        OIDField.setEditable(true);
        
        valueLabel = new JLabel("Value for additional variable:");
        valueField = new JTextField(20);
        valueField.setEditable(true);
        
        communityLabel = new JLabel("Community:");
        communityField = new JTextField(20);
        communityField.setText("public");
        communityField.setEditable(true);
        
        authorLabel = new JLabel(" Version 1.1        J. Sevy, January 2001 ");
        authorLabel.setFont(new Font("SansSerif", Font.ITALIC, 8));
            
        
        sendv1TrapButton = new JButton("Send v1 trap");
        sendv1TrapButton.setActionCommand("send v1 trap");
        sendv1TrapButton.addActionListener(this);
        
        sendv2TrapButton = new JButton("Send v2 trap");
        sendv2TrapButton.setActionCommand("send v2 trap");
        sendv2TrapButton.addActionListener(this);
        
        sendv2InformRequestButton = new JButton("Send v2 inform request");
        sendv2InformRequestButton.setActionCommand("send v2 inform request");
        sendv2InformRequestButton.addActionListener(this);
        
        clearButton = new JButton("Clear messages");
        clearButton.setActionCommand("clear messages");
        clearButton.addActionListener(this);
        
        messagesArea = new JTextArea(10,60);
        messagesScroll = new JScrollPane(messagesArea);
        
        valueTypeBox = new JComboBox();
        valueTypeBox.addItem("SNMPInteger");
        valueTypeBox.addItem("SNMPCounter32");
        valueTypeBox.addItem("SNMPCounter64");
        valueTypeBox.addItem("SNMPGauge32");
        valueTypeBox.addItem("SNMPOctetString");
        valueTypeBox.addItem("SNMPIPAddress");
        valueTypeBox.addItem("SNMPNSAPAddress");
        valueTypeBox.addItem("SNMPObjectIdentifier");
        valueTypeBox.addItem("SNMPTimeTicks");
        valueTypeBox.addItem("SNMPUInteger32");
        
        
        
        enterpriseLabel = new JLabel("Enterprise OID:");
        enterpriseField = new JTextField(20);
        enterpriseField.setEditable(true);
        
        agentLabel = new JLabel("Agent IP address:");
        agentField = new JTextField(20);
        agentField.setEditable(true);
        
        genericTrapLabel = new JLabel("Generic trap:");
        genericTrapBox = new JComboBox();
        genericTrapBox.addItem("Cold start (0)");
        genericTrapBox.addItem("Warm start (1)");
        genericTrapBox.addItem("Link down (2)");
        genericTrapBox.addItem("Link up (3)");
        genericTrapBox.addItem("Authentication failure (4)");
        genericTrapBox.addItem("EGP neighbor loss (5)");
        genericTrapBox.addItem("Enterprise specific (6)");
        
        
        specificTrapLabel = new JLabel("Specific trap:");
        specificTrapBox = new JComboBox();
        specificTrapBox.addItem("0");
        specificTrapBox.addItem("1");
        specificTrapBox.addItem("2");
        specificTrapBox.addItem("3");
        specificTrapBox.addItem("4");
        specificTrapBox.addItem("4");
        

        
        // now set up display
        
        // set params for layout manager
        GridBagLayout  theLayout = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        
        c.gridwidth = 1;
        c.gridheight = 1;
        c.fill = GridBagConstraints.NONE;
        c.ipadx = 0;
        c.ipady = 0;
        c.insets = new Insets(2,2,2,2);
        c.anchor = GridBagConstraints.CENTER;
        c.weightx = 0;
        c.weighty = 0;
        
        
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(theLayout);
        
        c.gridx = 1;
        c.gridy = 1;
        theLayout.setConstraints(sendv1TrapButton, c);
        buttonPanel.add(sendv1TrapButton);
        
        c.gridx = 2;
        c.gridy = 1;
        theLayout.setConstraints(sendv2TrapButton, c);
        buttonPanel.add(sendv2TrapButton);
        
        c.gridx = 3;
        c.gridy = 1;
        theLayout.setConstraints(sendv2InformRequestButton, c);
        buttonPanel.add(sendv2InformRequestButton);
        
        JPanel hostPanel = new JPanel();
        hostPanel.setLayout(theLayout);
        
        c.gridx = 1;
        c.gridy = 1;
        theLayout.setConstraints(hostIDLabel, c);
        hostPanel.add(hostIDLabel);
        
        c.gridx = 2;
        c.gridy = 1;
        theLayout.setConstraints(hostIDField, c);
        hostPanel.add(hostIDField);
        
        c.gridx = 1;
        c.gridy = 2;
        theLayout.setConstraints(communityLabel, c);
        hostPanel.add(communityLabel);
        
        c.gridx = 2;
        c.gridy = 2;
        theLayout.setConstraints(communityField, c);
        hostPanel.add(communityField);
        
        
        
        JPanel oidPanel = new JPanel();
        oidPanel.setLayout(theLayout);
        
        c.gridx = 1;
        c.gridy = 1;
        theLayout.setConstraints(enterpriseLabel, c);
        oidPanel.add(enterpriseLabel);
        
        c.gridx = 2;
        c.gridy = 1;
        theLayout.setConstraints(enterpriseField, c);
        oidPanel.add(enterpriseField);
        
        c.gridx = 1;
        c.gridy = 2;
        theLayout.setConstraints(agentLabel, c);
        oidPanel.add(agentLabel);
        
        c.gridx = 2;
        c.gridy = 2;
        theLayout.setConstraints(agentField, c);
        oidPanel.add(agentField);
        
        c.gridx = 1;
        c.gridy = 3;
        theLayout.setConstraints(genericTrapLabel, c);
        oidPanel.add(genericTrapLabel);
        
        c.gridx = 2;
        c.gridy = 3;
        theLayout.setConstraints(genericTrapBox, c);
        oidPanel.add(genericTrapBox);
        
        c.gridx = 1;
        c.gridy = 4;
        theLayout.setConstraints(specificTrapLabel, c);
        oidPanel.add(specificTrapLabel);
        
        c.gridx = 2;
        c.gridy = 4;
        theLayout.setConstraints(specificTrapBox, c);
        oidPanel.add(specificTrapBox);
        
        c.gridx = 1;
        c.gridy = 5;
        theLayout.setConstraints(OIDLabel, c);
        oidPanel.add(OIDLabel);
        
        c.gridx = 2;
        c.gridy = 5;
        theLayout.setConstraints(OIDField, c);
        oidPanel.add(OIDField);
        
        c.gridx = 1;
        c.gridy = 6;
        theLayout.setConstraints(valueLabel, c);
        oidPanel.add(valueLabel);
        
        c.gridx = 2;
        c.gridy = 6;
        theLayout.setConstraints(valueField, c);
        oidPanel.add(valueField);
        
        c.gridx = 3;
        c.gridy = 6;
        theLayout.setConstraints(valueTypeBox, c);
        oidPanel.add(valueTypeBox);
        
        
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.CENTER;
        
        
        
        JPanel messagesPanel = new JPanel();
        messagesPanel.setLayout(theLayout);
        
        c.gridx = 1;
        c.gridy = 1;
        c.anchor = GridBagConstraints.WEST;
        JLabel messagesLabel = new JLabel("Sent traps:");
        theLayout.setConstraints(messagesLabel, c);
        messagesPanel.add(messagesLabel);
        
        c.gridx = 2;
        c.gridy = 1;
        c.anchor = GridBagConstraints.EAST;
        theLayout.setConstraints(clearButton, c);
        messagesPanel.add(clearButton);
        
        c.fill = GridBagConstraints.BOTH;
        c.gridx = 1;
        c.gridy = 2;
        c.gridwidth = 2;
        c.weightx = .5;
        c.weighty = .5;
        c.anchor = GridBagConstraints.CENTER;
        theLayout.setConstraints(messagesScroll, c);
        messagesPanel.add(messagesScroll);
        
        
        c.gridwidth = 1;
        c.weightx = 0;
        c.weighty = 0;
        
        
        
        this.getContentPane().setLayout(theLayout);
        
        
        c.gridx = 1;
        c.gridy = 1;
        theLayout.setConstraints(hostPanel, c);
        this.getContentPane().add(hostPanel);
        
        c.gridx = 1;
        c.gridy = 2;
        theLayout.setConstraints(oidPanel, c);
        this.getContentPane().add(oidPanel);
        
        c.gridx = 1;
        c.gridy = 3;
        theLayout.setConstraints(buttonPanel, c);
        this.getContentPane().add(buttonPanel);
        
        c.fill = GridBagConstraints.BOTH;
        c.gridx = 1;
        c.gridy = 4;
        c.weightx = .5;
        c.weighty = .5;
        theLayout.setConstraints(messagesPanel, c);
        this.getContentPane().add(messagesPanel);
        
        c.fill = GridBagConstraints.NONE;
        c.gridx = 1;
        c.gridy = 5;
        c.weightx = 0;
        c.weighty = 0;
        theLayout.setConstraints(authorLabel, c);
        this.getContentPane().add(authorLabel);
        
        
    }
    
    
    
    
    
    public void actionPerformed(ActionEvent theEvent)
    // respond to button pushes, menu selections
    {
        String command = theEvent.getActionCommand();
        
    
        if (command == "quit")
        {
            System.exit(0);
        }
        
        
        
        if (command == "clear messages")
        {
            messagesArea.setText("");
        }
        
        
        
        if (command == "about")
        {
            //AboutDialog aboutDialog = new AboutDialog(this);
        }
        
        
        if (command == "send v1 trap")
        {
            try
            {
            
                String community = communityField.getText();
                int version = 0;    // SNMPv1
                InetAddress hostAddress = InetAddress.getByName(hostIDField.getText());
                
                
                
                SNMPObjectIdentifier enterpriseOID = new SNMPObjectIdentifier(enterpriseField.getText());
                SNMPIPAddress agentAddress = new SNMPIPAddress(agentField.getText());
                int genericTrap = genericTrapBox.getSelectedIndex();
                int specificTrap = specificTrapBox.getSelectedIndex();
                SNMPTimeTicks timestamp = new SNMPTimeTicks((long)(System.currentTimeMillis()/10));
                
                // see if have any additional variable pairs to send, and add them to
                // the VarBindList if so
                SNMPVarBindList varBindList = new SNMPVarBindList();
                
                String itemIDString = OIDField.getText();
                
                if (!itemIDString.equals(""))
                {
                    SNMPObjectIdentifier itemID = new SNMPObjectIdentifier(itemIDString);
                    
                    String valueString = valueField.getText();
                    String valueTypeString = (String)valueTypeBox.getSelectedItem();
                    valueTypeString = "libsnmp." + valueTypeString;
                    
                    SNMPObject itemValue;
                    Class valueClass = Class.forName(valueTypeString);
                    itemValue = (SNMPObject)valueClass.newInstance();
                    itemValue.setValue(valueString);
                    
                    varBindList.addSNMPObject(new SNMPVariablePair(itemID, itemValue));
                }
                
                // create trap pdu
                SNMPv1TrapPDU pdu = new SNMPv1TrapPDU(enterpriseOID, agentAddress, genericTrap, specificTrap, timestamp, varBindList);
    
                // and send it
                messagesArea.append("Sending trap to " + hostIDField.getText() + ":\n");
        
                messagesArea.append("  enterprise OID:     " + pdu.getEnterpriseOID().toString() + "\n");
                messagesArea.append("  agent address:      " + pdu.getAgentAddress().toString() + "\n");
                messagesArea.append("  generic trap:       " + pdu.getGenericTrap() + "\n");
                messagesArea.append("  specific trap:      " + pdu.getSpecificTrap() + "\n");
                messagesArea.append("  timestamp:          " + pdu.getTimestamp() + "\n");
                messagesArea.append("  supplementary vars: " + pdu.getVarBindList().toString() + "\n");
                
                messagesArea.append("\n");
                
                
                trapSenderInterface.sendTrap(hostAddress, community, pdu);
            
            }
            catch(InterruptedIOException e)
            {
                messagesArea.append("Interrupted during trap send:  " + e + "\n");
            }
            catch(Exception e)
            {
                messagesArea.append("Exception during trap send:  " + e + "\n");
            }
        }
        
        
        if (command == "send v2 trap")
        {
            try
            {
            
                String community = communityField.getText();
                InetAddress hostAddress = InetAddress.getByName(hostIDField.getText());
                
                
                // use the enterprise OID field as the libsnmp trap OID
                SNMPObjectIdentifier snmpTrapOID = new SNMPObjectIdentifier(enterpriseField.getText());
                
                // let uptime just be system time...
                SNMPTimeTicks sysUptime = new SNMPTimeTicks((long)(System.currentTimeMillis()/10));
                
                // see if have any additional variable pairs to send, and add them to
                // the VarBindList if so
                SNMPVarBindList varBindList = new SNMPVarBindList();
                
                String itemIDString = OIDField.getText();
                
                if (!itemIDString.equals(""))
                {
                    SNMPObjectIdentifier itemID = new SNMPObjectIdentifier(itemIDString);
                    
                    String valueString = valueField.getText();
                    String valueTypeString = (String)valueTypeBox.getSelectedItem();
                    valueTypeString = "libsnmp." + valueTypeString;
                    
                    SNMPObject itemValue;
                    Class valueClass = Class.forName(valueTypeString);
                    itemValue = (SNMPObject)valueClass.newInstance();
                    itemValue.setValue(valueString);
                    
                    varBindList.addSNMPObject(new SNMPVariablePair(itemID, itemValue));
                }
                
                // create trap pdu
                SNMPv2TrapPDU pdu = new SNMPv2TrapPDU(sysUptime, snmpTrapOID, varBindList);
    
                // and send it
                messagesArea.append("Sending trap to " + hostIDField.getText() + ":\n");
        
                messagesArea.append("  system uptime:      " + pdu.getSysUptime().toString() + "\n");
                messagesArea.append("  trap OID:           " + pdu.getSNMPTrapOID().toString() + "\n");
                messagesArea.append("  var bind list:      " + pdu.getVarBindList().toString() + "\n");
                
                messagesArea.append("\n");
                
                
                trapSenderInterface.sendTrap(hostAddress, community, pdu);
            
            }
            catch(InterruptedIOException e)
            {
                messagesArea.append("Interrupted during trap send:  " + e + "\n");
            }
            catch(Exception e)
            {
                messagesArea.append("Exception during trap send:  " + e + "\n");
            }
        }
        
        
        if (command == "send v2 inform request")
        {
            try
            {
            
                String community = communityField.getText();
                InetAddress hostAddress = InetAddress.getByName(hostIDField.getText());
                
                
                // use the enterprise OID field as the libsnmp trap OID
                SNMPObjectIdentifier snmpTrapOID = new SNMPObjectIdentifier(enterpriseField.getText());
                
                // let uptime just be system time...
                SNMPTimeTicks sysUptime = new SNMPTimeTicks((long)(System.currentTimeMillis()/10));
                
                // see if have any additional variable pairs to send, and add them to
                // the VarBindList if so
                SNMPVarBindList varBindList = new SNMPVarBindList();
                
                String itemIDString = OIDField.getText();
                
                if (!itemIDString.equals(""))
                {
                    SNMPObjectIdentifier itemID = new SNMPObjectIdentifier(itemIDString);
                    
                    String valueString = valueField.getText();
                    String valueTypeString = (String)valueTypeBox.getSelectedItem();
                    valueTypeString = "libsnmp." + valueTypeString;
                    
                    SNMPObject itemValue;
                    Class valueClass = Class.forName(valueTypeString);
                    itemValue = (SNMPObject)valueClass.newInstance();
                    itemValue.setValue(valueString);
                    
                    varBindList.addSNMPObject(new SNMPVariablePair(itemID, itemValue));
                }
                
                // create inform request pdu
                SNMPv2InformRequestPDU pdu = new SNMPv2InformRequestPDU(sysUptime, snmpTrapOID, varBindList);
    
                // and send it
                messagesArea.append("Sending inform request to " + hostIDField.getText() + ":\n");
        
                messagesArea.append("  system uptime:      " + pdu.getSysUptime().toString() + "\n");
                messagesArea.append("  trap OID:           " + pdu.getSNMPTrapOID().toString() + "\n");
                messagesArea.append("  var bind list:      " + pdu.getVarBindList().toString() + "\n");
                
                messagesArea.append("\n");
                
                
                informRequestSenderInterface.sendInformRequest(hostAddress, community, pdu);
            
            }
            catch(InterruptedIOException e)
            {
                messagesArea.append("Interrupted during inform request send:  " + e + "\n");
            }
            catch(Exception e)
            {
                messagesArea.append("Exception during inform request send:  " + e + "\n");
            }
        }
        
    }
    
      
    
    
    public static void main(String args[]) 
    {
        try
        {
            SNMPTrapSendTest theApp = new SNMPTrapSendTest();
            theApp.pack();
            theApp.setSize(700,500);
            theApp.setVisible(true);
        }
        catch (Exception e)
        {}
    }
    

}