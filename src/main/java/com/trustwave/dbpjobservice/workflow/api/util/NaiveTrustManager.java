package com.trustwave.dbpjobservice.workflow.api.util;

import javax.net.ssl.X509TrustManager;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class NaiveTrustManager implements X509TrustManager {
    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[0];
    }

    @Override
    public void checkClientTrusted(X509Certificate[] x509CertificateArray,
            String string) throws CertificateException {
    }

    @Override
    public void checkServerTrusted(X509Certificate[] x509CertificateArray,
            String string) throws CertificateException {
    }
}
