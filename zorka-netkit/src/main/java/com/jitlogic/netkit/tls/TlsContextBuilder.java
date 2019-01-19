/*
 * Copyright (c) 2012-2019 Rafał Lewczuk All Rights Reserved.
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

package com.jitlogic.netkit.tls;

import com.jitlogic.netkit.log.Logger;
import com.jitlogic.netkit.log.LoggerFactory;
import com.jitlogic.netkit.util.NetkitUtil;

import javax.net.ssl.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.util.Map;
import java.util.Properties;

public class TlsContextBuilder {

    private static Logger log = LoggerFactory.getLogger(TlsContextBuilder.class);

    private SSLContext context;
    private Properties props;

    private KeyManager[] keyManagers = null;
    private TrustManager[] trustManagers = null;

    public TlsContextBuilder(Properties props) {
        this.props = props;
    }

    public SSLContext get() {
        if (context == null) {
            setup();
        }
        return context;
    }

    private void setupKeyStore() {
        String keyStorePath = props.getProperty("keystore");

        if (keyStorePath != null) {
            File keyStoreFile = new File(keyStorePath);
            if (!keyStoreFile.exists()) {
                log.error("Cannot initialize TLS: file " + keyStorePath + " is missing.");
                return;
            }

            String keyStorePass = props.getProperty("keystore.pass", "changeit");

            InputStream is = null;
            try {
                KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
                is = new FileInputStream(keyStoreFile);
                keystore.load(is, keyStorePass.toCharArray());
                KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                kmf.init(keystore, keyStorePass.toCharArray());
                TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                tmf.init(keystore);
                keyManagers = kmf.getKeyManagers();
                trustManagers = tmf.getTrustManagers();
            } catch (Exception e) {
                log.error("Cannot load TLS key file: " + keyStorePath, e);
            } finally {
                NetkitUtil.close(is);
            }
        }

    }

    private void setupTrustStore() {
        String trustStorePath = props.getProperty("truststore");

        if (trustStorePath != null) {

            File trustStoreFile = new File(trustStorePath);
            if (!trustStoreFile.exists()) {
                log.error("Cannot initialize TLS for client : file " + trustStorePath + " is missing. Service  will not start.");
                return;
            }

            String trustStorePass = props.getProperty("truststore.pass", "changeit");

            try {
                TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                tmf.init((KeyStore) null);

                X509TrustManager defaultTM = lookupTM(tmf);

                KeyStore localTS = KeyStore.getInstance(KeyStore.getDefaultType());

                FileInputStream is = null;
                try {
                    is = new FileInputStream(trustStoreFile);
                    localTS.load(is, trustStorePass.toCharArray());
                } catch (IOException e) {
                    log.error("Cannot load trust store file " + trustStoreFile, e);
                    return;
                } finally {
                    NetkitUtil.close(is);
                }

                tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                tmf.init(localTS);

                X509TrustManager localTM = lookupTM(tmf);

                if (defaultTM == null || localTM == null) {
                    log.error("Cannot initialize TLS: either default or local trust manager is null.");
                    return;
                }

                trustManagers = new TrustManager[] { new TlsTrustManager(defaultTM, localTM) };
            } catch (Exception e) {
                log.error("Trust store did not configure properly", e);
            }
        }
    }

    private X509TrustManager lookupTM(TrustManagerFactory tmf) {
        X509TrustManager tm = null;
        for (TrustManager t : tmf.getTrustManagers()) {
            if (t instanceof X509TrustManager) {
                tm = (X509TrustManager) t;
                break;
            }
        }
        return tm;
    }

    private void setup() {
        setupKeyStore();
        setupTrustStore();
        try {
            context = SSLContext.getInstance("TLS");
            context.init(keyManagers, trustManagers, null);
        } catch (Exception e) {
            log.error("Cannot initialize TLS context", e);
        }
    }

    public static SSLContext ctx(String...args) {
        Properties props = new Properties();
        for (int i = 1; i < args.length; i+=2) {
            props.setProperty(args[i-1], args[i]);
        }
        return new TlsContextBuilder(props).get();
    }

    public static SSLContext svrContext(String keystorePath, String keystorePass) {
        return ctx("keystore", keystorePath, "keystore.pass", keystorePass);
    }

    public static SSLContext fromMap(String prefix, Map<String,String> conf) {
        if (conf.containsKey(prefix + "keystore")) {
            Properties props = new Properties();
            props.setProperty("keystore", conf.get(prefix + "keystore"));
            if (conf.containsKey(prefix + "keystore.pass"))
                props.setProperty("keystore.pass", conf.get(prefix + "keystore.pass"));
            return new TlsContextBuilder(props).get();
        } else {
            return null;
        }
    }
}
