package com.jitlogic.zorka.core.integ.zabbix;

import com.jitlogic.zorka.common.util.ZorkaUtil;
import com.jitlogic.zorka.core.ZorkaBshAgent;
import com.jitlogic.zorka.core.integ.QueryTranslator;
import com.jitlogic.zorka.net.TcpSessionFactory;
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
