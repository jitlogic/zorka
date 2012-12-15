/**
 * Copyright 2012 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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
package com.jitlogic.zorka.integ.snmp;

import com.jitlogic.zorka.libsnmp.SNMPObjectIdentifier;

public class TrapVarBindDef {
    private int type;
    private String oidSuffix;
    private int stage;
    private int slot;


    public TrapVarBindDef(int[] slot, int type, String oidSuffix) {
        this.type = type;
        this.stage = slot[0];
        this.slot = slot[1];
        this.oidSuffix = oidSuffix;
    }


    public int getType() {
        return type;
    }

    public int getStage() {
        return stage;
    }


    public int getSlot() {
        return slot;
    }


    public String getOidSuffix() {
        return oidSuffix;
    }

}
