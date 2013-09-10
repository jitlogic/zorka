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
package com.jitlogic.zorka.central.client;

import com.jitlogic.zorka.central.data.TraceTemplateInfo;
import org.fusesource.restygwt.client.MethodCallback;
import org.fusesource.restygwt.client.RestService;

import javax.ws.rs.*;
import java.util.List;
import java.util.Map;


public interface TraceAdminService extends RestService {

    @GET
    @Path("admin/templates")
    public void listTemplates(MethodCallback<List<TraceTemplateInfo>> cb);

    @POST
    @Path("admin/templates")
    public void saveTemplate(TraceTemplateInfo tti, MethodCallback<Integer> cb);

    @GET
    @Path("admin/tidmap")
    public void getTidMap(MethodCallback<Map<String, String>> cb);

    @DELETE
    @Path("admin/templates/{tid}")
    public void removeTemplate(@PathParam("tid") int tid, MethodCallback<Void> cb);
}
