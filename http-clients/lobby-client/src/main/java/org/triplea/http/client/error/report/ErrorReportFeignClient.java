package org.triplea.http.client.error.report;

import feign.HeaderMap;
import feign.Headers;
import feign.RequestLine;
import java.util.Map;
import org.triplea.http.client.HttpConstants;

@SuppressWarnings("InterfaceNeverImplemented")
@Headers({HttpConstants.CONTENT_TYPE_JSON, HttpConstants.ACCEPT_JSON})
interface ErrorReportFeignClient {
  @RequestLine("POST " + ErrorReportClient.ERROR_REPORT_PATH)
  ErrorReportResponse uploadErrorReport(
      @HeaderMap Map<String, Object> headers, ErrorReportRequest request);

  @RequestLine("POST " + ErrorReportClient.CAN_UPLOAD_ERROR_REPORT_PATH)
  CanUploadErrorReportResponse canUploadErrorReport(
      @HeaderMap Map<String, Object> headers, CanUploadRequest canUploadRequest);
}
