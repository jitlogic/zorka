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

/**
 * JEDI stands for JSON Extensible Data Interface.
 */
public interface JediService {

    public Object GET(String path, Map<String,String> params);

    public Object PUT(String path, Map<String,String> params, Object data);

    public Object DELETE(String path, Map<String,String> params);

    public Object POST(String path, Map<String,String> params, Object data);
}
