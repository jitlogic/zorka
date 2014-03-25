/**
 * Copyright 2012-2014 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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
package com.jitlogic.zico.client.panel;


import com.jitlogic.zico.shared.data.HostListObject;
import com.jitlogic.zico.shared.data.HostProxy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HostGroup implements HostListObject {

    private boolean expanded = true;
    private String name;

    private Set<HostProxy> hosts = new HashSet<HostProxy>();

    public HostGroup(String name) {
        this.name = name;
    }

    public void addHost(HostProxy host) {
        hosts.add(host);
    }

    public List<HostProxy> getHosts() {
        List<HostProxy> ret = new ArrayList<HostProxy>(hosts.size());

        ret.addAll(hosts);

        Collections.sort(ret, new Comparator<HostProxy>() {
            @Override
            public int compare(HostProxy o1, HostProxy o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });

        return ret;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getAddr() {
        return "";
    }

    @Override
    public boolean isEnabled() {
        return false;
    }

    public boolean isExpanded() {
        return expanded;
    }

    public int size() {
        return hosts.size();
    }

    public void clear() {
        hosts.clear();
    }

    public void toggleExpanded() {
        expanded = !expanded;
    }
}
