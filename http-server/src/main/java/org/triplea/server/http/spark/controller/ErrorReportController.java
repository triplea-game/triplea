package org.triplea.server.http.spark.controller;

import static spark.Spark.post;

import java.util.function.Function;

import org.triplea.http.client.error.report.create.ErrorReport;
import org.triplea.http.client.error.report.create.ErrorReportResponse;
import org.triplea.server.reporting.error.ErrorReportRequest;

import com.google.gson.Gson;

import lombok.AllArgsConstructor;
import spark.Request;

/**
 * Spark http controller that binds the error upload endpoint with the error report upload handler.
 */
@AllArgsConstructor
public class ErrorReportController implements Runnable {

  public static final String ERROR_REPORT_PATH = "/error-report";

  private final Function<ErrorReportRequest, ErrorReportResponse> errorReportIngestion;

  @Override
  public void run() {
    post(ERROR_REPORT_PATH, (req, res) -> uploadErrorReport(req));
  }

  String uploadErrorReport(final Request req) {
    final ErrorReportRequest errorReport = readErrorReport(req);
    final ErrorReportResponse result = errorReportIngestion.apply(errorReport);
    return new Gson().toJson(result);
  }

  private static ErrorReportRequest readErrorReport(final Request request) {
    return ErrorReportRequest.builder()
        .errorReport(new Gson().fromJson(request.body(), ErrorReport.class))
        .clientIp(request.ip())
        .build();
  }
}
