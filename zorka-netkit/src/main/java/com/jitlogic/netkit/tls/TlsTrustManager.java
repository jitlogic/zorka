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
