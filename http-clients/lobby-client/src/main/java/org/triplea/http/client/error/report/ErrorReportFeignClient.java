package org.triplea.http.client.error.report;

import feign.Headers;
import feign.RequestLine;
import org.triplea.http.client.HttpConstants;

@SuppressWarnings("InterfaceNeverImplemented")
@Headers({HttpConstants.CONTENT_TYPE_JSON, HttpConstants.ACCEPT_JSON})
interface ErrorReportFeignClient {
  @RequestLine("POST " + ErrorReportClient.ERROR_REPORT_PATH)
  ErrorReportResponse uploadErrorReport(ErrorReportRequest request);

  @RequestLine("POST " + ErrorReportClient.CAN_UPLOAD_ERROR_REPORT_PATH)
  CanUploadErrorReportResponse canUploadErrorReport(CanUploadRequest canUploadRequest);
}
