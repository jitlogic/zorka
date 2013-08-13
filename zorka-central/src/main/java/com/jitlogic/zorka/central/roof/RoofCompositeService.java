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
package com.jitlogic.zorka.central.roof;


import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RoofCompositeService implements RoofService {

    private Map<String,RoofService> services = new LinkedHashMap<String, RoofService>();


    public void register(String path, RoofService service) {
        services.put(path, service);
    }

    @Override
    public Object GET(List<String> path, Map<String, String> params) {
        RoofService svc = services.get(path.get(0));

        if (svc == null) {
            throw new RoofException(404, "No service found: " + path);
        }

        return svc.GET(path.subList(1, path.size()), params);
    }


    @Override
    public Object PUT(List<String> path, Map<String, String> params, Object data) {
        RoofService svc = services.get(path.get(0));

        if (svc == null) {
            throw new RoofException(404, "No service found.");
        }

        return svc.PUT(path.subList(1, path.size()), params, data);
    }


    @Override
    public Object DELETE(List<String> path, Map<String, String> params) {
        RoofService svc = services.get(path.get(0));

        if (svc == null) {
            throw new RoofException(404, "No service found.");
        }

        return svc.DELETE(path.subList(1, path.size()), params);
    }


    @Override
    public Object POST(List<String> path, Map<String, String> params, Object data) {
        RoofService svc = services.get(path.get(0));

        if (svc == null) {
            throw new RoofException(404, "No service found.");
        }

        return svc.POST(path.subList(1, path.size()), params, data);
    }
}
