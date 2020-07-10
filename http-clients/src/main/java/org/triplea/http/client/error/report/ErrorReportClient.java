package org.triplea.http.client.error.report;

import feign.FeignException;
import java.net.URI;
import java.util.Map;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.triplea.http.client.HttpClient;
import org.triplea.http.client.SystemIdHeader;

/** Http client to upload error reports to the http lobby server. */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ErrorReportClient {
  public static final String ERROR_REPORT_PATH = "/error-report";
  public static final String CAN_UPLOAD_ERROR_REPORT_PATH = "/error-report-check";
  public static final int MAX_REPORTS_PER_DAY = 5;

  private final Map<String, Object> headers;
  private final ErrorReportFeignClient errorReportFeignClient;

  /** Creates an error report uploader clients, sends error reports and gets a response back. */
  public static ErrorReportClient newClient(final URI uri) {
    return new ErrorReportClient(
        SystemIdHeader.headers(), new HttpClient<>(ErrorReportFeignClient.class, uri).get());
  }

  /**
   * API to upload an exception error report from a TripleA client to TripleA server.
   *
   * @throws FeignException Thrown on non-2xx responses.
   */
  public ErrorReportResponse uploadErrorReport(final ErrorReportRequest request) {
    return errorReportFeignClient.uploadErrorReport(headers, request);
  }

  /**
   * Checks if user can upload a request. A request can be uploaded if: - it has not yet been
   * reported - reporting version is greater than fix version - user is not banned
   */
  public CanUploadErrorReportResponse canUploadErrorReport(
      final CanUploadRequest canUploadRequest) {
    return errorReportFeignClient.canUploadErrorReport(headers, canUploadRequest);
  }
}
