/** 
 * Copyright 2012-2015 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
 * 
 * ZORKA is free software. You can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * ZORKA is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * ZORKA. If not, see <http://www.gnu.org/licenses/>.
 */

package com.jitlogic.zorka.core.integ.zabbix;


import java.net.Socket;

import com.jitlogic.zorka.core.ZorkaBshAgent;
import com.jitlogic.zorka.common.util.ZorkaConfig;
import com.jitlogic.zorka.core.integ.AbstractTcpAgent;
import com.jitlogic.zorka.core.integ.QueryTranslator;
import com.jitlogic.zorka.core.integ.ZorkaRequestHandler;

/**
 * Zabbix agent integrates Zorka with Zabbix server. It handles incoming zabbix
 * requests and forwards to BSH agent.
 *
 * @author rafal.lewczuk@jtlogic.com
 */
public class ZabbixAgent extends AbstractTcpAgent {

    /**
     * Creates zabbix agent.
     *
     * @param agent BSH agent
     */
	public ZabbixAgent(ZorkaConfig config, ZorkaBshAgent agent, QueryTranslator translator) {
        super(config, agent, translator, "zabbix", "127.0.0.1", 10055);
    }


    @Override
    protected ZorkaRequestHandler newRequest(Socket sock) {
        return new ZabbixRequestHandler(sock, translator);
    }

}
