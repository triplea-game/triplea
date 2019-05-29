package org.triplea.http.client.error.report;

import java.net.URI;

import org.triplea.http.client.HttpClient;

import com.google.common.annotations.VisibleForTesting;

import feign.FeignException;
import feign.Headers;
import feign.RequestLine;

/**
 * Http client to upload error reports to the http lobby server.
 */
@SuppressWarnings("InterfaceNeverImplemented")
public interface ErrorUploadClient {

  @VisibleForTesting
  String ERROR_REPORT_PATH = "/error-report";

  /**
   * API to upload an exception error report from a TripleA client to TripleA server.
   *
   * @throws FeignException Thrown on non-2xx responses.
   */
  @RequestLine("POST " + ERROR_REPORT_PATH)
  @Headers({
      "Content-Type: application/json",
      "Accept: application/json"
  })
  ErrorUploadResponse uploadErrorReport(ErrorUploadRequest request);

  /**
   * Creates an error report uploader clients, sends error reports and gets a response back.
   */
  static ErrorUploadClient newClient(final URI uri) {
    return new HttpClient<>(ErrorUploadClient.class, uri).get();
  }
}
