package org.triplea.server.error.reporting;

import java.util.function.Function;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.triplea.http.client.error.report.ErrorUploadRequest;
import org.triplea.http.client.error.report.ErrorUploadResponse;

import lombok.AllArgsConstructor;

/**
 * Http controller that binds the error upload endpoint with the error report upload handler.
 */
@AllArgsConstructor
@Path(ErrorReportController.ERROR_REPORT_PATH)
@Produces(MediaType.APPLICATION_JSON)
public class ErrorReportController {

  static final String ERROR_REPORT_PATH = "/error-report";

  private final Function<ErrorReportRequest, ErrorUploadResponse> errorReportIngestion;

  @POST
  public ErrorUploadResponse uploadErrorReport(
      @Context final HttpServletRequest req,
      final ErrorUploadRequest errorReport) {

    if (errorReport.getBody() == null || errorReport.getTitle() == null) {
      throw new IllegalArgumentException("Missing error report body and/or title");
    }

    return errorReportIngestion.apply(ErrorReportRequest.builder()
        .clientIp(extractClientIp(req))
        .errorReport(errorReport)
        .build());
  }

  private String extractClientIp(final HttpServletRequest request) {
    final String forwarded = request.getHeader("X-Forwarded-For");
    if (forwarded != null) {
      return forwarded;
    }

    return request.getRemoteHost();
  }
}
