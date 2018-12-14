/*
 * Copyright (c) 2012-2018 Rafał Lewczuk All Rights Reserved.
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

package com.jitlogic.netkit.http;

import com.jitlogic.netkit.ArgumentException;
import com.jitlogic.netkit.NetException;
import com.jitlogic.netkit.log.EventSink;
import com.jitlogic.netkit.log.LoggerFactory;
import com.jitlogic.netkit.tls.TlsContextBuilder;
import com.jitlogic.netkit.util.BufStreamOutput;
import com.jitlogic.netkit.util.NetkitUtil;

import javax.net.SocketFactory;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.channels.SelectionKey;
import java.util.Map;
import java.util.regex.Matcher;

import static com.jitlogic.netkit.http.HttpProtocol.REG_URL_PROTO;
import static com.jitlogic.netkit.http.HttpProtocol.RE_URL;

/**
 * HTTP client using traditional
 */
public class HttpStreamClient implements HttpMessageListener, HttpMessageClient {

    private int port;
    private boolean tls;
    private InetAddress host;

    private Socket socket;
    private String baseUri;
    private HttpConfig config;

    private SocketFactory socketFactory;

    private BufStreamOutput stream;
    private HttpMessageHandler output;
    private HttpStreamInput input;

    private HttpMessage result;

    private static EventSink evtConnects = LoggerFactory.getSink("http.client.connects");
    private static EventSink evtCalls = LoggerFactory.getSink("http.client.connects");


    public HttpStreamClient(HttpConfig config, String baseUrl) {
        this.config = config;

        Matcher m = RE_URL.matcher(baseUrl);
        if (!m.matches()) throw new NetException("Invalid URL: " + baseUrl);

        this.tls = "https".equalsIgnoreCase(m.group(1));
        this.port = (m.group(3) != null) ? Integer.parseInt(m.group(3).substring(1)) : tls ? 443 : 80;
        try {
            this.host = InetAddress.getByName(m.group(2));
        } catch (UnknownHostException e) {
            throw new NetException("Error resolving host: " + m.group(2));
        }

        this.baseUri = m.group(4) != null ? m.group(4) : "/";

        if (!tls) {
            socketFactory = SocketFactory.getDefault();
        } else {
            socketFactory = config.getSslContext().getSocketFactory();
        }
    }


    @Override
    public HttpMessage exec(HttpMessage req) {

        if (socket == null || !socket.isConnected()) {
            connect();
        }

        Exception e = null;

        for (int i = 0; i < config.getMaxRetries(); i++) {
            try {
                output.submit(new HttpEncoder(config, stream), null, req);
                input.run();
                evtCalls.call();
                return result;
            } catch (Exception e1) {
                e = e1;
                reconnect();
            }
        }

        throw new NetException("Error executing HTTP call", e);
    }


    private void reconnect() {
        if (socket != null) {
            NetkitUtil.close(socket);
            socket = null;
            input = null;
            stream = null;
            output = null;
        }
        connect();
    }


    private void connect() {
        try {
            socket = socketFactory.createSocket(host, port);
            input = new HttpStreamInput(config, this, HttpDecoderState.READ_RESP_LINE, socket.getInputStream());
            stream = new BufStreamOutput(socket.getOutputStream());
            output = new HttpMessageHandler(config, null);
            evtConnects.call();
        } catch (IOException e) {
            evtConnects.error();
            throw new NetException("Cannot connect to " + host + ":" + port, e);
        }
    }


    @Override
    public void submit(SelectionKey key, HttpMessage message) {
        this.result = message;
    }


    public static HttpStreamClient fromMap(Map<String,String> conf) {
        String url = conf.get("http.url");

        Matcher m = RE_URL.matcher(url);

        if (!m.matches()) {
            throw new ArgumentException("Invalid URL: " + url);
        }

        HttpConfig httpConfig = new HttpConfig();

        if ("https".equalsIgnoreCase(m.group(REG_URL_PROTO))) {
            httpConfig.setSslContext(TlsContextBuilder.fromMap("http.", conf));
        }

        return new HttpStreamClient(httpConfig, url);
    }

}
