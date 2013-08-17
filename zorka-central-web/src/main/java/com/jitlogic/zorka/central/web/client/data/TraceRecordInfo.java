/**
 * Copyright 2012-2013 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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
package com.jitlogic.zorka.central.web.client.data;


import org.codehaus.jackson.annotate.JsonProperty;

public class TraceRecordInfo {

    @JsonProperty
    long CALLS;

    @JsonProperty
    long ERRORS;

    @JsonProperty
    long TIME;

    @JsonProperty
    int FLAGS;

    @JsonProperty
    String METHOD;

    @JsonProperty
    int CHILDREN;

    @JsonProperty
    String PATH;

    public long getCalls() {
        return CALLS;
    }

    public long getErorrs() {
        return ERRORS;
    }

    public long getTime() {
        return TIME;
    }

    public int getFlags() {
        return FLAGS;
    }

    public String getMethod() {
        return METHOD;
    }

    public int getChildren() {
        return CHILDREN;
    }

    public String getPath() {
        return PATH;
    }
}
