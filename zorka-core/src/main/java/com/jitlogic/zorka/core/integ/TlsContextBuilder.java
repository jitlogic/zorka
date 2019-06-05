package com.jitlogic.zorka.core.integ;

import com.jitlogic.zorka.common.util.ZorkaConfig;
import com.jitlogic.zorka.common.util.ZorkaUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;

public class TlsContextBuilder {

    private static final Logger log = LoggerFactory.getLogger(TlsContextBuilder.class);

    private String prefix;
    private ZorkaConfig config;
    private SSLContext context;

    private KeyManager[] keyManagers = null;
    private TrustManager[] trustManagers = null;

    public TlsContextBuilder(ZorkaConfig config, String prefix) {
        this.config = config;
        this.prefix = prefix;
    }


    public SSLContext build() {
        if (context == null) {
            setup();
        }
        return context;
    }

    private void setupKeyStore() {
        String keyStorePath = config.stringCfg(prefix + ".tls.keystore",
                new File(config.getHomeDir(), "zorka.jks").getPath());

        if (keyStorePath != null) {
            File keyStoreFile = new File(keyStorePath);
            if (!keyStoreFile.exists()) {
                log.error("Cannot initialize TLS for service '{}': file {} is missing", prefix, keyStorePath);
                return;
            }

            String keyStorePass = config.stringCfg(prefix + ".tls.keystore.pass", "changeit");

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
                log.error("Cannot load TLS key for '{}'", prefix, e);
            } finally {
                ZorkaUtil.close(is);
            }
        }
    }

    private void setupTrustStore() {

        String trustStorePath = config.stringCfg(prefix + ".tls.truststore", null);

        if (trustStorePath != null) {

            File trustStoreFile = new File(trustStorePath);
            if (!trustStoreFile.exists()) {
                log.error("Cannot initialize TLS for client {}: file {} is missing.", prefix, trustStorePath);
                return;
            }

            String trustStorePass = config.stringCfg(prefix + ".tls.truststore.pass", "changeit");

            try {
                TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                tmf.init((KeyStore) null);

                X509TrustManager defaultTM = null;
                for (TrustManager t : tmf.getTrustManagers()) {
                    if (t instanceof X509TrustManager) {
                        defaultTM = (X509TrustManager) t;
                        break;
                    }
                }

                KeyStore localTS = KeyStore.getInstance(KeyStore.getDefaultType());

                FileInputStream is = null;
                try {
                    is = new FileInputStream(trustStoreFile);
                    localTS.load(is, trustStorePass.toCharArray());
                } catch (IOException e) {
                    log.error("Cannot load trust store file {}", trustStoreFile, e);
                    return;
                } finally {
                    ZorkaUtil.close(is);
                }

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

                trustManagers = new TrustManager[] { new TlsTrustManager(defaultTM, localTM) };
            } catch (Exception e) {
                log.error("Trust store for '{}' did not configure properly", prefix, e);
            }
        }
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
}
