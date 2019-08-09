package org.triplea.http.client.error.report;

import feign.FeignException;
import feign.Headers;
import feign.RequestLine;
import java.net.URI;
import org.triplea.http.client.HttpClient;
import org.triplea.http.client.HttpConstants;

/** Http client to upload error reports to the http lobby server. */
@SuppressWarnings("InterfaceNeverImplemented")
public interface ErrorUploadClient {

  String ERROR_REPORT_PATH = "/error-report";
  String CAN_REPORT_PATH = "/can-submit-error-report";

  /**
   * API to upload an exception error report from a TripleA client to TripleA server.
   *
   * @throws FeignException Thrown on non-2xx responses.
   */
  @RequestLine("POST " + ERROR_REPORT_PATH)
  @Headers({HttpConstants.CONTENT_TYPE_JSON, HttpConstants.ACCEPT_JSON})
  ErrorUploadResponse uploadErrorReport(ErrorUploadRequest request);

  @RequestLine("GET " + CAN_REPORT_PATH)
  boolean canSubmitErrorReport();

  /** Creates an error report uploader clients, sends error reports and gets a response back. */
  static ErrorUploadClient newClient(final URI uri) {
    return new HttpClient<>(ErrorUploadClient.class, uri).get();
  }
}
