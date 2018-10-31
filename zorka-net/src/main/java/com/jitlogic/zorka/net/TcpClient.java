/*
 * Copyright (c) 2012-2017 Rafał Lewczuk All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jitlogic.zorka.net;

import com.jitlogic.zorka.common.util.ZorkaConfig;
import com.jitlogic.zorka.common.util.ZorkaRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.SocketFactory;
import javax.net.ssl.*;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

public class TcpClient {

    private static final Logger log = LoggerFactory.getLogger(TcpClient.class);

    private String prefix;
    private ZorkaConfig config;

    private InetAddress addr;
    private int port;

    private SocketFactory socketFactory;

    public TcpClient(ZorkaConfig config, String prefix) {
        this.config = config;
        this.prefix = prefix;

        try {
            setup();
        } catch (Exception e) {
            log.error("Cannot configure TCP client for " + prefix, e);
        }
    }

    private void setup() throws Exception {

        String sa = config.stringCfg(prefix + ".addr", null);

        if (sa.contains(":")) {
          String[] ap = sa.split(":");
          addr = InetAddress.getByName(ap[0]);
        }

        if (port == 0) {
            port = config.intCfg(prefix + ".port", port);
        }

        boolean tlsEnabled = config.boolCfg(prefix + ".tls", false);

        if (!tlsEnabled) {
            socketFactory = SocketFactory.getDefault();
        } else {
            SSLContext ctx = new TlsContextBuilder(config, prefix).build();
            if (ctx != null) {
                socketFactory = ctx.getSocketFactory();
            } else {
                log.error("Cannot initialize SSL context due to previous errors.");
            }
        }
    }

    private void checkInitialized() {
        if (socketFactory == null) {
            throw new ZorkaRuntimeException("Cannot connect from client '" + prefix
                    + "': not configured properly, see previous errors.");
        }
    }

    public Socket connect() throws IOException {
        checkInitialized();
        return socketFactory.createSocket(addr, port);
    }

    public Socket connect(InetAddress addr, int port) throws IOException {
        checkInitialized();
        return socketFactory.createSocket(addr, port);
    }

    public Socket connect(String addr, int port) throws IOException {
        checkInitialized();
        return socketFactory.createSocket(addr, port);
    }
}
