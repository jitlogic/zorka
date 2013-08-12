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
package com.jitlogic.zorka.central.jedi;


import java.util.LinkedHashMap;
import java.util.Map;

public class JediCompositeService implements JediService {

    private Map<String,JediService> services = new LinkedHashMap<String, JediService>();


    public void register(String path, JediService service) {
        services.put(path, service);
    }


    private Map.Entry<String,JediService> lookup(String path) {
        for (Map.Entry<String,JediService> e : services.entrySet()) {
            if (path.startsWith(e.getKey())) {
                return e;
            }
        }
        return null;
    }

    private String subpath(String path, String prefix) {
        String p = path.substring(prefix.length());
        return p.startsWith("/") ? p.substring(1) : p;
    }

    @Override
    public Object GET(String path, Map<String, String> params) {
        Map.Entry<String,JediService> entry = lookup(path);

        if (entry == null) {
            throw new JediException(404, "No service found: " + path);
        }

        return entry.getValue().GET(subpath(path, entry.getKey()), params);
    }


    @Override
    public Object PUT(String path, Map<String, String> params, Object data) {
        Map.Entry<String,JediService> entry = lookup(path);

        if (entry == null) {
            throw new JediException(404, "No service found.");
        }

        return entry.getValue().PUT(subpath(path, entry.getKey()), params, data);
    }


    @Override
    public Object DELETE(String path, Map<String, String> params) {
        Map.Entry<String,JediService> entry = lookup(path);

        if (entry == null) {
            throw new JediException(404, "No service found.");
        }

        return entry.getValue().DELETE(subpath(path, entry.getKey()), params);
    }


    @Override
    public Object POST(String path, Map<String, String> params, Object data) {
        Map.Entry<String,JediService> entry = lookup(path);

        if (entry == null) {
            throw new JediException(404, "No service found.");
        }

        return entry.getValue().POST(subpath(path, entry.getKey()), params, data);
    }
}
