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
package com.jitlogic.zorka.common.test.support;

import com.jitlogic.zorka.common.tracedata.HelloRequest;
import com.jitlogic.zorka.common.zico.ZicoDataProcessor;
import com.jitlogic.zorka.common.zico.ZicoDataProcessorFactory;
import com.jitlogic.zorka.common.zico.ZicoException;
import com.jitlogic.zorka.common.zico.ZicoPacket;

import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;


public class TestZicoProcessorFactory implements ZicoDataProcessorFactory {

    private Map<String,TestZicoProcessor> pmap = new HashMap<String, TestZicoProcessor>();

    @Override
    public ZicoDataProcessor get(Socket socket, HelloRequest hello) throws IOException {

        if (hello == null || "BAD".equals(hello.getAuth())) {
            throw new ZicoException(ZicoPacket.ZICO_AUTH_ERROR, "Login failed.");
        }

        if (!pmap.containsKey(hello.getHostname())) {
            pmap.put(hello.getHostname(), new TestZicoProcessor());
        }

        return pmap.get(hello.getHostname());
    }

    public Map<String, TestZicoProcessor> getPmap() {
        return pmap;
    }

}
