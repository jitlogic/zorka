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
import com.jitlogic.zorka.common.util.ZorkaUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.SocketFactory;
import javax.net.ssl.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

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
            return;
        }

        String trustStorePath = config.stringCfg(prefix + ".tls.truststore", null);

        if (trustStorePath == null) {
            socketFactory = SSLSocketFactory.getDefault();
            return;
        }

        File trustStoreFile = new File(trustStorePath);
        if (!trustStoreFile.exists()) {
            log.error("Cannot initialize TLS for client " + prefix + ": file " + trustStorePath + " is missing. Service " + prefix + " will not start.");
            return;
        }

        String trustStorePass = config.stringCfg(prefix + ".tls.truststore.pass", "changeit");

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init((KeyStore)null);

        X509TrustManager defaultTM = null;
        for (TrustManager t : tmf.getTrustManagers()) {
            if (t instanceof X509TrustManager) {
                defaultTM = (X509TrustManager)t;
                break;
            }
        }

        FileInputStream is = new FileInputStream(trustStoreFile);
        KeyStore localTS = KeyStore.getInstance(KeyStore.getDefaultType());
        localTS.load(is, trustStorePass.toCharArray());
        ZorkaUtil.close(is);

        tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(localTS);

        X509TrustManager localTM = null;
        for (TrustManager t : tmf.getTrustManagers()) {
            if (t instanceof X509TrustManager) {
                localTM = (X509TrustManager)t;
                break;
            }
        }

        if (defaultTM == null || localTM == null) {
            log.error("Cannot initialize TLS: either default or local trust manager is null.");
            return;
        }

        final X509TrustManager dtm = defaultTM, ltm = localTM;

        X509TrustManager tm = new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
                try {
                    ltm.checkServerTrusted(x509Certificates, s);
                } catch (CertificateException e) {
                    dtm.checkServerTrusted(x509Certificates, s);
                }
            }

            @Override
            public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
                try {
                    ltm.checkServerTrusted(x509Certificates, s);
                } catch (CertificateException e) {
                    dtm.checkServerTrusted(x509Certificates, s);
                }
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                X509Certificate[] dai = dtm.getAcceptedIssuers(), lai = ltm.getAcceptedIssuers();
                X509Certificate[] ai = new X509Certificate[dai.length+lai.length];

                if (dai.length > 0) System.arraycopy(dai, 0, ai, 0, dai.length);
                if (lai.length > 0) System.arraycopy(lai, 0, ai, dai.length, lai.length);

                return ai;
            }
        };

        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(null, new TrustManager[] { tm }, null);
        socketFactory = ctx.getSocketFactory();
    }

    public Socket connect() throws IOException {
        return socketFactory.createSocket(addr, port);
    }
    
}
