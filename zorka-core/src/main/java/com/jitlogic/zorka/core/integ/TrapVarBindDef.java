/**
 * Copyright 2012-2015 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
 * <p/>
 * This is free software. You can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * <p/>
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this software. If not, see <http://www.gnu.org/licenses/>.
 */
package com.jitlogic.zorka.core.integ;

/**
 * Contains information used to bind record fields to SNMP trap attributes.
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class TrapVarBindDef {

    /** SNMP data snmpDataType */
    private int snmpDataType;

    /** OID suffix */
    private String oidSuffix;

    /** Source field name */
    private String sourceField;


    /**
     * Creates record attribute to snmp trap attribute binding.
     *
     * @param sourceField source field
     *
     * @param snmpDataType SNMP data snmpDataType
     *
     * @param oidSuffix OID suffix
     */
    public TrapVarBindDef(String sourceField, int snmpDataType, String oidSuffix) {
        this.snmpDataType = snmpDataType;
        this.sourceField = sourceField;
        this.oidSuffix = oidSuffix;
    }


    /**
     * Returns SNMP data snmpDataType
     *
     * @return SNMP data snmpDataType
     */
    public int getSnmpDataType() {
        return snmpDataType;
    }


    /**
     * Returns source field name
     *
     * @return source field name
     */
    public String getSourceField() {
        return sourceField;
    }


    /**
     * Returns OID suffix.
     *
     * @return OID suffix
     */
    public String getOidSuffix() {
        return oidSuffix;
    }

}
