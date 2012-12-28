/**
 * Copyright 2012 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
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
package com.jitlogic.zorka.integ.nagios;

import com.jitlogic.zorka.agent.ZorkaBshAgent;
import com.jitlogic.zorka.agent.ZorkaConfig;
import com.jitlogic.zorka.integ.ZorkaRequestHandler;
import com.jitlogic.zorka.util.TcpServiceThread;

import java.net.Socket;

public class NagiosAgent extends TcpServiceThread {


    public NagiosAgent(ZorkaBshAgent agent) {
        super(agent, ZorkaConfig.getProperties(), "nagios", 5669);
    }

    @Override
    protected ZorkaRequestHandler newRequest(Socket sock) {
        return new NrpeRequestHandler(sock);
    }

}
