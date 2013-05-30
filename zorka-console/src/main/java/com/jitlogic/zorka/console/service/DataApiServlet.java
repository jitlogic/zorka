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
package com.jitlogic.zorka.console.service;

import com.jitlogic.zorka.core.util.JSONWriter;
import com.jitlogic.zorka.core.util.ZorkaUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DataApiServlet extends HttpServlet {


    private String rootURI = "/";
    private List<DataApiService> services = new ArrayList<DataApiService>();



    public void add(DataApiService service) {
        services.add(service);
    }


    @Override
    public void init(javax.servlet.ServletConfig config) {
        if (config.getInitParameter("rootURI") != null) {
            this.rootURI = config.getInitParameter("rootURI");
        }



    }


    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        response.setContentType("application/json;charset=UTF-8");
        String uri = request.getRequestURI();

        for (DataApiService service : services) {
            if (service.getUriPattern().matcher(uri).matches()) {
                response.getWriter().println(new JSONWriter().write(
                        service.get(uri, request.getParameterMap())));
                return;
            }
        }

        response.getWriter().println(new JSONWriter().write(
            ZorkaUtil.map("error", "Unregistered URI: " + uri)));

    }
}
