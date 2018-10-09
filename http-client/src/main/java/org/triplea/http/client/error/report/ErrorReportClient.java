package org.triplea.http.client.error.report;

import org.triplea.http.client.error.report.create.ErrorReport;
import org.triplea.http.client.error.report.create.ErrorReportResponse;

import com.google.common.annotations.VisibleForTesting;

import feign.Headers;
import feign.RequestLine;

@SuppressWarnings("InterfaceNeverImplemented")
interface ErrorReportClient {

  @VisibleForTesting
  String ERROR_REPORT_PATH = "/error-report";

  @RequestLine("POST " + ERROR_REPORT_PATH)
  @Headers({
      "Content-Type: application/json",
      "Accept: application/json"
  })
  ErrorReportResponse sendErrorReport(ErrorReport errorReport);
}
