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

import com.jitlogic.zorka.core.ZorkaBshAgent;
import com.jitlogic.zorka.common.util.ZorkaConfig;

import java.net.Socket;

/**
 * Nagios agent integrates Zorka with Nagios server. It handles incoming NRPE
 * requests, forwards them to BSH agent.
 *
 * @author rafal.lewczuk@jtlogic.com
 */
public class NagiosAgent extends AbstractTcpAgent {

    /**
     * Creates new Nagios agent.
     *
     * @param agent bsh agent
     */
    public NagiosAgent(ZorkaConfig config, ZorkaBshAgent agent, QueryTranslator translator) {
        super(config, agent, translator, "nagios", "127.0.0.1", 5669);
    }

    @Override
    protected ZorkaRequestHandler newRequest(Socket sock) {
        return new NrpeRequestHandler(sock, translator);
    }

}
