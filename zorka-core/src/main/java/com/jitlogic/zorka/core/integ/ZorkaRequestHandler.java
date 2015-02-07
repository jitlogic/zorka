/**
 * Copyright 2012-2015 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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
package com.jitlogic.zorka.core.integ;

import com.jitlogic.zorka.core.ZorkaCallback;

import java.io.IOException;

/**
 * Zorka request handler receives, parses incoming requests and sends replies.
 * Implementations of this interface handle specific protocols, eg. Zabbix or Nagios.
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public interface ZorkaRequestHandler extends ZorkaCallback {

    /**
     * Reads, parses and returns request string
     *
     * @return request (translated to BSH)
     *
     * @throws IOException if I/O error occurs
     */
    public String getReq() throws IOException;
}
