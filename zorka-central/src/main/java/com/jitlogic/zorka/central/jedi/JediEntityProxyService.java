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


import java.util.Map;
import java.util.regex.Pattern;

/**
 * Translates mapped HTTP requests into entity proxy method calls.
 */
public class JediEntityProxyService<T> implements JediService {

    private final static Pattern RE_SLASH = Pattern.compile("/");

    private JediEntityProxy<T> proxy;


    public JediEntityProxyService(JediEntityProxy<T> proxy) {
        this.proxy = proxy;
    }

    public JediEntityProxy<T> getProxy() {
        return proxy;
    }


    @Override
    public Object GET(String path, Map<String, String> params) {
        String id = path.trim();

        if (id.endsWith("/")) {
            id = id.substring(0, id.length()-1);
        }

        if (id.length() == 0) {
            return proxy.list(params);
        } else {
            return proxy.get(id, params);
        }
    }


    @Override
    public Object PUT(String path, Map<String, String> params, Object data) {
        throw new JediException(501, "Not implemented (yet).");
    }


    @Override
    public Object DELETE(String path, Map<String, String> params) {
        throw new JediException(501, "Not implemented (yet).");
    }


    @Override
    public Object POST(String path, Map<String, String> params, Object data) {
        throw new JediException(501, "Not implemented (yet).");
    }
}
