package com.jitlogic.zorka.console.service;

import com.jitlogic.zorka.core.util.RestfulService;

import java.util.Map;
import java.util.regex.Pattern;

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

public class DataApiService implements RestfulService<Object> {

    public Pattern getUriPattern() {
        return null;
    }

    @Override
    public Object get(String path, Map<String, String> params) {
        return null;
    }
}
