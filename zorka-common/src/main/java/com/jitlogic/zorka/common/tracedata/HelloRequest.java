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
package com.jitlogic.zorka.common.tracedata;


import com.jitlogic.zorka.common.util.ZorkaUtil;

public class HelloRequest {

    private long tstamp;

    private String hostname;

    private String auth;

    public HelloRequest(long tstamp, String hostname, String auth) {
        this.tstamp = tstamp;
        this.hostname = hostname;
        this.auth = auth;
    }

    public long getTstamp() {
        return tstamp;
    }

    public void setTstamp(long tstamp) {
        this.tstamp = tstamp;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getAuth() {
        return auth;
    }

    public void setAuth(String auth) {
        this.auth = auth;
    }

    @Override
    public String toString() {
        return "HelloRecord(" + tstamp + ", " + hostname + ")";
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof HelloRequest
            && tstamp == ((HelloRequest)obj).tstamp
            && ZorkaUtil.objEquals(hostname, ((HelloRequest)obj).hostname);
    }

    @Override
    public int hashCode() {
        return (int)(31 * tstamp + 17 * hostname.hashCode());
    }
}
