package org.triplea.http.client.error.report;

import feign.FeignException;
import feign.RequestLine;
import java.net.URI;
import org.triplea.http.client.HttpClient;
import org.triplea.http.client.lib.HttpClientHeaders;

/** Http client to upload error reports to the http lobby server. */
public interface ErrorReportClient {
  String ERROR_REPORT_PATH = "/game-support/error-report";
  String CAN_UPLOAD_ERROR_REPORT_PATH = "/game-support/error-report-check";
  int MAX_REPORTS_PER_DAY = 5;

  /** Creates an error report uploader clients, sends error reports and gets a response back. */
  static ErrorReportClient newClient(URI uri, String clientVersion) {
    return HttpClient.newClient(
        ErrorReportClient.class,
        uri,
        HttpClientHeaders.defaultHeadersWithClientVersion(clientVersion));
  }

  /**
   * API to upload an exception error report from a TripleA client to TripleA server.
   *
   * @throws FeignException Thrown on non-2xx responses.
   */
  @RequestLine("POST " + ErrorReportClient.ERROR_REPORT_PATH)
  ErrorReportResponse uploadErrorReport(ErrorReportRequest request);

  /**
   * Checks if user can upload a request. A request can be uploaded if: - it has not yet been
   * reported - reporting version is greater than fix version - user is not banned
   */
  @RequestLine("POST " + ErrorReportClient.CAN_UPLOAD_ERROR_REPORT_PATH)
  CanUploadErrorReportResponse canUploadErrorReport(CanUploadRequest canUploadRequest);
}
