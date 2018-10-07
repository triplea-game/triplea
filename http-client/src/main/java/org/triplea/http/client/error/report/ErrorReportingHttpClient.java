package org.triplea.http.client.error.report;

import java.net.URI;

import com.google.common.annotations.VisibleForTesting;

import feign.Feign;
import feign.Headers;
import feign.Logger;
import feign.Request;
import feign.RequestLine;
import feign.gson.GsonDecoder;
import feign.gson.GsonEncoder;
import org.triplea.http.client.error.report.json.message.ErrorReport;
import org.triplea.http.client.error.report.json.message.ErrorReportResponse;

@SuppressWarnings("InterfaceNeverImplemented")
interface ErrorReportingHttpClient {

  @VisibleForTesting
  String ERROR_REPORT_PATH = "/error-report";
  /**
   * How long we can take to start receiving a message.
   */
  int DEFAULT_CONNECT_TIMEOUT = 5 * 1000;
  /**
   * How long we can spend receiving a message.
   */
  int DEFAULT_READ_TIME_OUT = 20 * 1000;

  @RequestLine("POST " + ERROR_REPORT_PATH)
  @Headers({
      "Content-Type: application/json",
      "Accept: application/json"
  })
  ErrorReportResponse sendErrorReport(ErrorReport errorReport);

  static ErrorReportingHttpClient newClient(final URI hostUri) {
    return newClient(hostUri, DEFAULT_CONNECT_TIMEOUT, DEFAULT_READ_TIME_OUT);
  }

  @VisibleForTesting
  static ErrorReportingHttpClient newClient(
      final URI hostUri,
      int connectTimeoutMillis,
      int readTimeoutMillis) {
    return Feign.builder()
        .encoder(new GsonEncoder())
        .decoder(new GsonDecoder())
        .logger(new Logger.JavaLogger())
        .logLevel(Logger.Level.FULL)
        .options(new Request.Options(connectTimeoutMillis, readTimeoutMillis))
        .target(ErrorReportingHttpClient.class, hostUri.toString());
  }

}
