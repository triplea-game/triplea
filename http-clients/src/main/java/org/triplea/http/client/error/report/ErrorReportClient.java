package org.triplea.http.client.error.report;

import feign.FeignException;
import feign.Headers;
import feign.RequestLine;
import java.net.URI;
import org.triplea.http.client.HttpClient;
import org.triplea.http.client.HttpConstants;

/** Http client to upload error reports to the http lobby server. */
@SuppressWarnings("InterfaceNeverImplemented")
@Headers({HttpConstants.CONTENT_TYPE_JSON, HttpConstants.ACCEPT_JSON})
public interface ErrorReportClient {

  String ERROR_REPORT_PATH = "/error-report";

  int MAX_REPORTS_PER_DAY = 5;

  /**
   * API to upload an exception error report from a TripleA client to TripleA server.
   *
   * @throws FeignException Thrown on non-2xx responses.
   */
  @RequestLine("POST " + ERROR_REPORT_PATH)
  ErrorReportResponse uploadErrorReport(ErrorReportRequest request);

  /** Creates an error report uploader clients, sends error reports and gets a response back. */
  static ErrorReportClient newClient(final URI uri) {
    return new HttpClient<>(ErrorReportClient.class, uri).get();
  }
}
