package org.triplea.server.http.spark.controller;

import static spark.Spark.post;

import java.util.function.Function;

import org.triplea.http.client.error.report.ErrorUploadRequest;
import org.triplea.http.client.error.report.ErrorUploadResponse;
import org.triplea.server.reporting.error.CreateErrorReportException;
import org.triplea.server.reporting.error.ErrorReportRequest;

import com.google.gson.Gson;

import lombok.AllArgsConstructor;
import spark.Request;

/**
 * Spark http controller that binds the error upload endpoint with the error report upload handler.
 */
@AllArgsConstructor
public class ErrorReportController implements Runnable {

  private static final String ERROR_REPORT_PATH = "/error-report";

  private final Function<ErrorReportRequest, ErrorUploadResponse> errorReportIngestion;

  @Override
  public void run() {
    post(ERROR_REPORT_PATH, (req, res) -> uploadErrorReport(req));
  }

  private String uploadErrorReport(final Request req) {
    final org.triplea.server.reporting.error.ErrorReportRequest errorReport = readErrorReport(req);
    try {
      final ErrorUploadResponse result = errorReportIngestion.apply(errorReport);
      return new Gson().toJson(result);
    } catch (final CreateErrorReportException e) {
      return e.getMessage();
    }
  }

  private static ErrorReportRequest readErrorReport(final Request request) {
    return ErrorReportRequest.builder()
        .errorReport(new Gson().fromJson(request.body(), ErrorUploadRequest.class))
        .clientIp(request.ip())
        .build();
  }
}
