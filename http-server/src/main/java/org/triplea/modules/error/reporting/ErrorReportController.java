package org.triplea.modules.error.reporting;

import es.moki.ratelimij.dropwizard.annotation.Rate;
import es.moki.ratelimij.dropwizard.annotation.RateLimited;
import es.moki.ratelimij.dropwizard.filter.KeyPart;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import lombok.Builder;
import org.triplea.http.HttpController;
import org.triplea.http.client.error.report.ErrorReportClient;
import org.triplea.http.client.error.report.ErrorReportRequest;
import org.triplea.http.client.error.report.ErrorReportResponse;

/** Http controller that binds the error upload endpoint with the error report upload handler. */
@Builder
public class ErrorReportController extends HttpController {
  @Nonnull
  private final BiFunction<String, ErrorReportRequest, ErrorReportResponse> errorReportIngestion;

  @POST
  @Path(ErrorReportClient.ERROR_REPORT_PATH)
  @RateLimited(
      keys = {KeyPart.IP},
      rates = {
        @Rate(limit = ErrorReportClient.MAX_REPORTS_PER_DAY, duration = 1, timeUnit = TimeUnit.DAYS)
      })
  public ErrorReportResponse uploadErrorReport(
      @Context final HttpServletRequest request, final ErrorReportRequest errorReport) {

    if (errorReport.getBody() == null || errorReport.getTitle() == null) {
      throw new IllegalArgumentException("Missing error report body and/or title");
    }

    return errorReportIngestion.apply(request.getRemoteAddr(), errorReport);
  }
}
