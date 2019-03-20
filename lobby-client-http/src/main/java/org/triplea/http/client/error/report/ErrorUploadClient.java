package org.triplea.http.client.error.report;

import com.google.common.annotations.VisibleForTesting;

import feign.Headers;
import feign.RequestLine;

@SuppressWarnings("InterfaceNeverImplemented")
interface ErrorUploadClient {

  @VisibleForTesting
  String ERROR_REPORT_PATH = "/error-report";

  @RequestLine("POST " + ERROR_REPORT_PATH)
  @Headers({
      "Content-Type: application/json",
      "Accept: application/json"
  })
  ErrorUploadResponse sendErrorReport(ErrorUploadRequest errorReport);
}
