/*
 * Copyright 2014 Daniel Makoto Iguchi <daniel.iguchi@gmail.com>
 * Copyright 2012-2019 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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

package com.jitlogic.zorka.core.integ.zabbix;

import com.jitlogic.zorka.common.util.ZorkaUtil;
import com.jitlogic.zorka.core.ZorkaBshAgent;
import com.jitlogic.zorka.core.integ.QueryTranslator;
import com.jitlogic.zorka.core.integ.TcpSessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.Socket;

public class ZabbixConnectionHandler implements TcpSessionFactory {

    private static final Logger log = LoggerFactory.getLogger(ZabbixConnectionHandler.class);

    private ZorkaBshAgent agent;
    private QueryTranslator translator;

    public ZabbixConnectionHandler(ZorkaBshAgent agent, QueryTranslator translator) {
        this.agent = agent;
        this.translator = translator;
    }

    private void handleRequest(Socket socket) {
        try {
            ZabbixRequestHandler zch = new ZabbixRequestHandler(socket, translator);
            String req = zch.getReq();
            agent.exec(req, zch);
        } catch (IOException e) {
            log.error("Error handling Zabbix Agent request", e);
            ZorkaUtil.close(socket);
        }
    }

    @Override
    public Runnable getSession(final Socket socket) {
        return new Runnable() {
            @Override
            public void run() {
                handleRequest(socket);
            }
        };
    }
}
