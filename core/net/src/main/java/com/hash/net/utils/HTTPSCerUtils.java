package com.hash.net.utils;

import android.content.Context;

import java.io.InputStream;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import okhttp3.OkHttpClient;

public class HTTPSCerUtils {

    //只信任指定证书（传入raw资源ID）
    public static void setCertificate(Context context, OkHttpClient.Builder okHttpClientBuilder, int cerResID) {
        try (InputStream inputStream = context.getResources().openRawResource(cerResID)) {
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            Certificate ca = certificateFactory.generateCertificate(inputStream);

            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null, null);
            keyStore.setCertificateEntry("ca", ca);

            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(keyStore);

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, tmf.getTrustManagers(), new SecureRandom());
            okHttpClientBuilder.sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) tmf.getTrustManagers()[0]);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to configure custom certificate", e);
        }
    }

    //批量信任证书
    public static void setCertificates(Context context, OkHttpClient.Builder okHttpClientBuilder, int... cerResIDs) {
        try {
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");

            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null, null);
            for (int i = 0; i < cerResIDs.length; i++) {
                try (InputStream inputStream = context.getResources().openRawResource(cerResIDs[i])) {
                    Certificate ca = certificateFactory.generateCertificate(inputStream);
                    keyStore.setCertificateEntry("ca" + i, ca);
                }
            }

            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(keyStore);

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, tmf.getTrustManagers(), new SecureRandom());
            okHttpClientBuilder.sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) tmf.getTrustManagers()[0]);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to configure custom certificates", e);
        }
    }
}
