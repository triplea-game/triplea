package org.triplea.spitfire.server.controllers;

import java.net.URI;
import java.util.function.Function;
import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import lombok.Builder;
import org.jdbi.v3.core.Jdbi;
import org.triplea.http.client.AuthenticationHeaders;
import org.triplea.http.client.error.report.CanUploadErrorReportResponse;
import org.triplea.http.client.error.report.CanUploadRequest;
import org.triplea.http.client.error.report.ErrorReportClient;
import org.triplea.http.client.error.report.ErrorReportRequest;
import org.triplea.http.client.error.report.ErrorReportResponse;
import org.triplea.http.client.github.GithubApiClient;
import org.triplea.modules.LobbyModuleConfig;
import org.triplea.modules.error.reporting.CanUploadErrorReportStrategy;
import org.triplea.modules.error.reporting.CreateIssueParams;
import org.triplea.modules.error.reporting.CreateIssueStrategy;
import org.triplea.spitfire.server.HttpController;

/** Http controller that binds the error upload endpoint with the error report upload handler. */
@Builder
public class ErrorReportController extends HttpController {
  @Nonnull private final Function<CreateIssueParams, ErrorReportResponse> errorReportIngestion;
  @Nonnull private final Function<CanUploadRequest, CanUploadErrorReportResponse> canReportModule;

  /** Factory method. */
  public static ErrorReportController build(
      final LobbyModuleConfig configuration, final Jdbi jdbi) {

    final boolean errorReportStubbingMode = !configuration.isErrorReportToGithubEnabled();

    final GithubApiClient githubApiClient =
        GithubApiClient.builder()
            .uri(URI.create(configuration.getGithubWebServiceUrl()))
            .authToken(configuration.getGithubApiToken())
            .stubbingModeEnabled(errorReportStubbingMode)
            .build();

    return ErrorReportController.builder()
        .errorReportIngestion(
            CreateIssueStrategy.build(
                configuration.getGithubOrgForErrorReports(),
                configuration.getGithubRepoForErrorReports(),
                githubApiClient,
                jdbi))
        .canReportModule(CanUploadErrorReportStrategy.build(jdbi))
        .build();
  }

  @POST
  @Path(ErrorReportClient.CAN_UPLOAD_ERROR_REPORT_PATH)
  public CanUploadErrorReportResponse canUploadErrorReport(
      final CanUploadRequest canUploadRequest) {
    if (canUploadRequest == null
        || canUploadRequest.getErrorTitle() == null
        || canUploadRequest.getGameVersion() == null) {
      throw new IllegalArgumentException("Missing request attributes title or game version");
    }

    return canReportModule.apply(canUploadRequest);
  }

  /**
   * Endpoint where users can submit an error report, the server will use an API token of a generic
   * user to in turn create a github issue using the data from the error report.
   */
  @POST
  @Path(ErrorReportClient.ERROR_REPORT_PATH)
  public ErrorReportResponse uploadErrorReport(
      @Context final HttpServletRequest request, final ErrorReportRequest errorReport) {

    if (errorReport == null
        || errorReport.getBody() == null
        || errorReport.getTitle() == null
        || errorReport.getGameVersion() == null) {
      throw new IllegalArgumentException("Missing attribute, body, title, or game version");
    }

    return errorReportIngestion.apply(
        CreateIssueParams.builder()
            .ip(request.getRemoteAddr())
            .systemId(request.getHeader(AuthenticationHeaders.SYSTEM_ID_HEADER))
            .errorReportRequest(errorReport)
            .build());
  }
}
