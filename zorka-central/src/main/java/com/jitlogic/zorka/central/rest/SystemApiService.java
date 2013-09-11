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
package com.jitlogic.zorka.central.rest;

import com.jitlogic.zorka.central.CentralApp;
import com.jitlogic.zorka.central.CentralInstance;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Path("system")
public class SystemApiService {

    private CentralInstance instance;

    public SystemApiService() {
        instance = CentralApp.getInstance();
    }

    public SystemApiService(CentralInstance instance) {
        this.instance = instance;
    }

    @GET
    @Path("/info")
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> systemInfo() {
        return instance.systemInfo();
    }

}
