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
package com.jitlogic.zorka.central.web.service;

import com.jitlogic.zorka.central.roof.RoofService;
import com.jitlogic.zorka.common.util.JSONWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class RoofServlet extends HttpServlet {

    private RoofService service = CentralApp.getInstance().getRoofService(); // TODO decouple this ...

    private static final Pattern RE_SLASH = Pattern.compile("/");

    public void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        List<String> segs = Arrays.asList(RE_SLASH.split(req.getRequestURI()));

        List<String> path = segs;

        do {
            path = path.subList(1, path.size());
        } while (path.size() > 0 && !path.get(0).equals("roof"));

        path = path.subList(1, path.size());

        Map<String,String> params = new HashMap<String, String>();

        for (Object key : req.getParameterMap().keySet()) {
            params.put(key.toString(), req.getParameter(key.toString()));
        }

        if (path.size() > 0) {
            Object rslt = service.GET(path, params);
            resp.getWriter().write(new JSONWriter().write(rslt));
        } else {
            resp.getWriter().println("Bad request.");
        }
    }

}
