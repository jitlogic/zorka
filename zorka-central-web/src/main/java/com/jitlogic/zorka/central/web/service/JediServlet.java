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

import com.jitlogic.zorka.central.jedi.JediService;
import com.jitlogic.zorka.common.util.JSONWriter;
import com.jitlogic.zorka.common.util.ZorkaUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class JediServlet extends HttpServlet {

    private JediService service = CentralApp.getInstance().getJediService(); // TODO decouple this ...

    private static final Pattern RE_SLASH = Pattern.compile("\\/");

    public void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        List<String> segs = Arrays.asList(RE_SLASH.split(req.getRequestURI()));

        String path = null;

        for (int i = 0; i < segs.size(); i++) {
            if ("jedi".equals(segs.get(i))) {
                path = ZorkaUtil.join("/", segs.subList(i+1, segs.size()));
            }
        }

        if (path != null) {
            Object rslt = service.GET(path, req.getParameterMap());
            resp.getWriter().write(new JSONWriter().write(rslt));
        } else {
            resp.getWriter().println("Bad request.");
        }
    }

}
