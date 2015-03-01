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
package com.jitlogic.zorka.core;


public class ZorkaControl implements ZorkaControlMBean {

    private String mbsName, objectName;
    private AgentInstance instance;


    public ZorkaControl(String mbsName, String objectName, AgentInstance instance) {
        this.mbsName = mbsName;
        this.objectName = objectName;
        this.instance = instance;
    }


    public String getMbsName() {
        return mbsName;
    }


    public String getObjectName() {
        return objectName;
    }


    @Override
    public String getHostname() {
        return instance.getZorkaLib().getHostname();
    }


    @Override
    public long getTracerMinMethodTime() {
        return instance.getTracerLib().getTracerMinMethodTime();
    }


    @Override
    public void setTracerMinMethodTime(long t) {
        instance.getTracerLib().setTracerMinMethodTime(t);
    }


    @Override
    public long getTracerMinTraceTime() {
        return instance.getTracerLib().getTracerMinTraceTime();
    }


    @Override
    public void setTracerMinTraceTime(long t) {
        instance.getTracerLib().setTracerMinTraceTime(t);
    }


    @Override
    public long getTracerMaxTraceRecords() {
        return instance.getTracerLib().getTracerMaxTraceRecords();
    }


    @Override
    public void setTracerMaxTraceRecords(long t) {
        instance.getTracerLib().setTracerMaxTraceRecords(t);
    }


    @Override
    public String listTracerIncludes() {
        return instance.getTracerLib().listIncludes();
    }


    @Override
    public void reload() {
        instance.reload();
    }
}
