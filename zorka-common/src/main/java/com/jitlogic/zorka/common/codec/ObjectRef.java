/*
 * Copyright 2016-2019 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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

package com.jitlogic.zorka.common.codec;

import com.jitlogic.zorka.common.cbor.CborDataWriter;
import com.jitlogic.zorka.common.cbor.CborObject;

import static com.jitlogic.zorka.common.cbor.TraceDataTags.TAG_STRING_REF;


/**
 *
 */
public class ObjectRef implements CborObject {

    int id;

    public ObjectRef(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    @Override
    public boolean equals(Object obj) {
        return obj != null && obj.getClass() == ObjectRef.class && ((ObjectRef)obj).id == id;
    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public String toString() {
        return "ref[" + id + "]";
    }

    @Override
    public void write(CborDataWriter writer) {
        writer.writeTag(TAG_STRING_REF);
        writer.writeInt(id);
    }

}
