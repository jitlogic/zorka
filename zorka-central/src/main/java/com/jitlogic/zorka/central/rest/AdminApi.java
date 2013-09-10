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
import com.jitlogic.zorka.central.TraceTemplateManager;
import com.jitlogic.zorka.central.data.TraceTemplateInfo;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.Map;

@Path("admin")
public class AdminApi {


    private TraceTemplateManager templater;

    public AdminApi() {
        templater = CentralApp.getInstance().getTemplater();
    }

    public AdminApi(CentralInstance instance) {
        templater = instance.getTemplater();
    }

    @GET
    @Path("/templates")
    @Produces(MediaType.APPLICATION_JSON)
    public List<TraceTemplateInfo> listTemplates() {
        return templater.listTemplates();
    }


    @POST
    @Path("/templates")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public int saveTemplate(TraceTemplateInfo tti) {
        return templater.save(tti);
    }

    @DELETE
    @Path("/templates/{tid}")
    public void removeTemplate(@PathParam("tid") int tid) {
        templater.remove(tid);
    }

    @GET
    @Path("/tidmap")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<Integer, String> getTidMap() {
        return templater.getTidMap();
    }
}
