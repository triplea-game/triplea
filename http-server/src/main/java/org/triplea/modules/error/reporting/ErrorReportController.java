package org.triplea.modules.error.reporting;

import com.google.common.base.Preconditions;
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
import org.jdbi.v3.core.Jdbi;
import org.triplea.http.AppConfig;
import org.triplea.http.HttpController;
import org.triplea.http.client.error.report.ErrorReportClient;
import org.triplea.http.client.error.report.ErrorReportRequest;
import org.triplea.http.client.error.report.ErrorReportResponse;
import org.triplea.http.client.github.issues.GithubIssueClient;

/** Http controller that binds the error upload endpoint with the error report upload handler. */
@Builder
public class ErrorReportController extends HttpController {
  @Nonnull
  private final BiFunction<String, ErrorReportRequest, ErrorReportResponse> errorReportIngestion;

  public static ErrorReportController build(final AppConfig configuration, final Jdbi jdbi) {
    final boolean isTest = configuration.getGithubApiToken().equals("test");

    final GithubIssueClient githubIssueClient =
        GithubIssueClient.builder()
            .uri(AppConfig.GITHUB_WEB_SERVICE_API_URL)
            .authToken(configuration.getGithubApiToken())
            .githubOrg(AppConfig.GITHUB_ORG)
            .githubRepo(configuration.getGithubRepo())
            .isTest(isTest)
            .build();

    if (isTest) {
      Preconditions.checkState(!configuration.isProd());
    }

    return ErrorReportController.builder()
        .errorReportIngestion(CreateIssueStrategy.build(githubIssueClient, jdbi))
        .build();
  }

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
