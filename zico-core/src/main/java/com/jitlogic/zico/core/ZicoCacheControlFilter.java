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
package com.jitlogic.zico.core;


import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;
import java.util.regex.Pattern;

public class ZicoCacheControlFilter implements Filter {

    private final static long DAY = 86400000;

    private final static Pattern[] CACHE_MATCHES = {
            Pattern.compile("^.*\\.cache\\.js$"),
            Pattern.compile("^.*\\.cache\\.html$"),
            Pattern.compile("^.*\\.cache\\.png$"),
    };


    private boolean matches(Pattern[] matches, String uri) {
        for (Pattern p : matches) {
            if (p.matcher(uri).matches()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (request instanceof HttpServletRequest && response instanceof HttpServletResponse) {
            HttpServletRequest req = (HttpServletRequest) request;
            HttpServletResponse res = (HttpServletResponse) response;
            Date now = new Date();
            res.setDateHeader("Date", now.getTime());
            if (matches(CACHE_MATCHES, req.getRequestURI())) {
                res.setDateHeader("Expires", now.getTime() + DAY * 300);
                res.setHeader("Pragma", "no-cache");
                res.setHeader("Cache-control", "public, max-age=" + (DAY / 1000 * 300));
            } else {
                res.setDateHeader("Expires", now.getTime() + DAY);
                res.setHeader("Pragma", "no-cache");
                res.setHeader("Cache-control", "no-cache, no-store, must-revalidate");
            }
        }
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
    }
}
