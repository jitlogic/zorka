package com.jitlogic.zorka.core.integ;

import javax.net.ssl.X509TrustManager;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class TlsTrustManager implements X509TrustManager {

    private X509TrustManager dtm;
    private X509TrustManager ltm;

    public TlsTrustManager(X509TrustManager dtm, X509TrustManager ltm) {
        this.dtm = dtm;
        this.ltm = ltm;
    }

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
}
