package com.jitlogic.netkit;

import javax.net.ssl.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;

public class TlsUtils {

    public static SSLContext svrContext(String keystorePath, String keystorePass) {
        InputStream is = null;
        try {
            is = new FileInputStream(keystorePath);
            KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
            keystore.load(is, keystorePass.toCharArray());
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(keystore, keystorePass.toCharArray());
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(keystore);
            KeyManager[] keyManagers = kmf.getKeyManagers();
            TrustManager[] trustManagers = tmf.getTrustManagers();
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(keyManagers, trustManagers, null);
            return context;
        } catch (Exception e) {
            throw new ArgumentException("Cannot open store " + keystorePath + ": " + e.getMessage());
        } finally {
            try {
                if (is != null) is.close();
            } catch (IOException e) {

            }
        }
    }

}
