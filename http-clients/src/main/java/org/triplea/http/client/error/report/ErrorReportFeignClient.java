package org.triplea.http.client.error.report;

import feign.FeignException;
import feign.HeaderMap;
import feign.Headers;
import feign.RequestLine;
import java.util.Map;
import org.triplea.http.client.HttpConstants;

@SuppressWarnings("InterfaceNeverImplemented")
@Headers({HttpConstants.CONTENT_TYPE_JSON, HttpConstants.ACCEPT_JSON})
interface ErrorReportFeignClient {
  /**
   * API to upload an exception error report from a TripleA client to TripleA server.
   *
   * @throws FeignException Thrown on non-2xx responses.
   */
  @RequestLine("POST " + ErrorReportClient.ERROR_REPORT_PATH)
  ErrorReportResponse uploadErrorReport(
      @HeaderMap Map<String, Object> headers, ErrorReportRequest request);
}
