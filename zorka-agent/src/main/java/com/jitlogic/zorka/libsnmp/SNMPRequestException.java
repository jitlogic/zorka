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


/**
*    Exception thrown when request to get or set the value of an SNMP OID on a device fails. Reason could be
*    that specified variable not supported by device, or that supplied community name has insufficient
*    privileges. errorStatus parameter allows the reason for the failure to be specified, and errorIndex
*   allows the index of the failed OID to be specified.
*/

public class SNMPRequestException extends SNMPException
{
    
    public static final int NO_ERROR = 0;
    public static final int VALUE_TOO_BIG = 1;
    public static final int VALUE_NOT_AVAILABLE = 2;
    public static final int BAD_VALUE = 3;
    public static final int VALUE_READ_ONLY = 4;
    public static final int FAILED = 5;
    
    public int errorIndex = 0;
    public int errorStatus = 0;
    
    
    
    /**
    *    Create exception with errorIndex, errorStatus
    */
    
    public SNMPRequestException(int errorIndex, int errorStatus)
    {
        super();
        
        this.errorIndex = errorIndex;
        this.errorStatus = errorStatus;
    }
    
    
    
    /**
    *    Create exception with errorIndex, errorStatus, and message string
    */
    
    public SNMPRequestException(String message, int errorIndex, int errorStatus)
    {
        super(message);
        
        this.errorIndex = errorIndex;
        this.errorStatus = errorStatus;
    }
    
}