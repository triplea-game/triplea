package org.triplea.game.client;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.logging.Level;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import games.strategy.triplea.settings.ClientSetting;
import lombok.extern.java.Log;

/**
 * Like {@code HeadedGameRunner} except turns on debug configuration for development purposes.
 */
@Log
public final class DevelopmentGameRunner {

  private DevelopmentGameRunner() {}

  public static void main(final String[] args) {
    turnOffSslCertificateValidation();
    ClientSetting.initialize();
    ClientSetting.showBetaFeatures.setValue(true);
    ClientSetting.showConsole.setValue(true);
    HeadedGameRunner.main(args);
  }

  private static void turnOffSslCertificateValidation() {
    try {
      final TrustManager[] trustAllCerts = new TrustManager[] {
          new X509TrustManager() {
            @Override
            public X509Certificate[] getAcceptedIssuers() {
              return new X509Certificate[0];
            }

            @Override
            public void checkClientTrusted(final X509Certificate[] certs, final String authType) {}

            @Override
            public void checkServerTrusted(final X509Certificate[] certs, final String authType) {}
          }
      };

      final SSLContext sc = SSLContext.getInstance("SSL");
      sc.init(null, trustAllCerts, new SecureRandom());
      HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
      HttpsURLConnection.setDefaultHostnameVerifier((arg0, arg1) -> true);
    } catch (final NoSuchAlgorithmException | KeyManagementException e) {
      log.log(Level.WARNING, "Failed to disable certs", e);
    }
  }
}
