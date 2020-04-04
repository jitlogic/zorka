/*
 * Copyright (c) 2012-2020 Rafał Lewczuk All Rights Reserved.
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

package com.jitlogic.zorka.common.http;

import com.jitlogic.zorka.common.stats.MethodCallStatistic;
import com.jitlogic.zorka.common.stats.MethodCallStatistics;
import com.jitlogic.zorka.common.util.TlsContextBuilder;
import com.jitlogic.zorka.common.util.ZorkaRuntimeException;
import com.jitlogic.zorka.common.util.ZorkaUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.SocketFactory;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.regex.Matcher;

import static com.jitlogic.zorka.common.http.HttpProtocol.REG_URL_PROTO;
import static com.jitlogic.zorka.common.http.HttpProtocol.RE_URL;

/**
 * HTTP client using streams.
 */
public class HttpClient implements HttpHandler, Closeable {

    private static Logger log = LoggerFactory.getLogger(HttpClient.class);

    private int port;
    private boolean tls;
    private InetAddress addr;
    private String host;

    private Socket socket;
    private BufferedOutputStream output;

    private String baseUri;

    private HttpConfig config;

    private SocketFactory socketFactory;

    private MethodCallStatistic evtConnects, evtCalls;


    public HttpClient(HttpConfig config, String baseUrl, MethodCallStatistics stats) {
        this.config = config;

        Matcher m = RE_URL.matcher(baseUrl);
        if (!m.matches()) throw new HttpException("Invalid URL: " + baseUrl);

        this.evtCalls = stats.getMethodCallStatistic("HttpClientCalls");
        this.evtConnects = stats.getMethodCallStatistic("HttpClientConnects");

        this.tls = "https".equalsIgnoreCase(m.group(REG_URL_PROTO));
        String pstr = m.group(HttpProtocol.REG_URL_PORT);
        this.port = (pstr != null) ? Integer.parseInt(pstr.substring(1)) : tls ? 443 : 80;
        try {
            this.host = m.group(HttpProtocol.REG_URL_ADDR);
            this.addr = InetAddress.getByName(this.host);
            this.config.setHost(this.host);
        } catch (UnknownHostException e) {
            throw new HttpException("Error resolving host: " + m.group(2));
        }

        this.baseUri = m.group(4) != null ? m.group(4) : "/";

        if (!tls) {
            socketFactory = SocketFactory.getDefault();
        } else {
            socketFactory = config.getSslContext().getSocketFactory();
        }
    }


    @Override
    public HttpMessage handle(HttpMessage req) {

        if (socket == null || !socket.isConnected()) {
            connect();
        }

        Exception e = null;



        for (int i = 0; i < config.getMaxRetries(); i++) {
            try {
                new HttpEncoder(config, baseUri, output).handle(req);
                output.flush();
                HttpMessage msg = new HttpDecoder(socket.getInputStream(), config).decode(true);
                evtCalls.logCall();
                return msg;
            } catch (Exception e1) {
                e = e1;
                reconnect();
            }
        }

        throw new HttpException("Error executing HTTP call", e);
    }


    private void reconnect() {
        close();
        connect();
    }


    @Override
    public void close() {
        if (socket != null) {
            ZorkaUtil.close(socket);
            socket = null;
            output = null;
        }
    }


    private void connect() {
        try {
            socket = socketFactory.createSocket(addr, port);
            output = new BufferedOutputStream(socket.getOutputStream());
            evtConnects.logCall();
        } catch (IOException e) {
            evtConnects.logError(1);
            throw new HttpException("Cannot connect to " + addr + ":" + port, e);
        }
    }


    public static HttpClient fromMap(Map<String,String> conf, MethodCallStatistics stats) {
        String url = conf.get("http.url");

        Matcher m = RE_URL.matcher(url);

        if (!m.matches()) {
            throw new ZorkaRuntimeException("Invalid URL: " + url);
        }

        HttpConfig httpConfig = new HttpConfig();

        if ("https".equalsIgnoreCase(m.group(REG_URL_PROTO))) {
            log.info("Creating TLS context for: {}", url);
            httpConfig.setSslContext(TlsContextBuilder.fromMap("http.", conf));
        }

        return new HttpClient(httpConfig, url, stats);
    }

}
