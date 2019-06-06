package org.triplea.server.error.reporting;

import java.time.Clock;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.jdbi.v3.core.Jdbi;
import org.triplea.http.client.error.report.ErrorUploadClient;
import org.triplea.http.client.error.report.ErrorUploadRequest;
import org.triplea.http.client.error.report.ErrorUploadResponse;
import org.triplea.http.client.github.issues.GithubIssueClient;
import org.triplea.lobby.server.db.ErrorReportingDao;
import org.triplea.server.http.AppConfig;

import com.google.common.base.Preconditions;

import lombok.Builder;

/**
 * Http controller that binds the error upload endpoint with the error report upload handler.
 */
@Builder
@Produces(MediaType.APPLICATION_JSON)
@Path("/")
public class ErrorReportController {
  @Nonnull
  private final Function<ErrorReportRequest, ErrorUploadResponse> errorReportIngestion;
  @Nonnull
  private final Predicate<String> errorReportRateChecker;

  @POST
  @Path(ErrorUploadClient.ERROR_REPORT_PATH)
  public ErrorUploadResponse uploadErrorReport(
      @Context final HttpServletRequest request,
      final ErrorUploadRequest errorReport) {

    if (errorReport.getBody() == null || errorReport.getTitle() == null) {
      throw new IllegalArgumentException("Missing error report body and/or title");
    }

    return errorReportIngestion.apply(ErrorReportRequest.builder()
        .clientIp(extractClientIp(request))
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

  /**
   * Checks if the user has hit their rate limit for submitting error reports.
   *
   * @return True if the user can submit an error report, false if they
   *         have hit their limit.
   */
  @GET
  @Path(ErrorUploadClient.CAN_REPORT_PATH)
  public boolean canSubmitErrorReport(@Context final HttpServletRequest request) {
    return errorReportRateChecker.test(extractClientIp(request));
  }
}
