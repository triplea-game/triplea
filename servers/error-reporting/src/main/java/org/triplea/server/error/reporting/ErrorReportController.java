package org.triplea.server.error.reporting;

import java.util.function.Function;
import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import lombok.Builder;
import org.jdbi.v3.core.Jdbi;
import org.triplea.dropwizard.common.IpAddressExtractor;
import org.triplea.http.client.error.report.CanUploadErrorReportResponse;
import org.triplea.http.client.error.report.CanUploadRequest;
import org.triplea.http.client.error.report.ErrorReportClient;
import org.triplea.http.client.error.report.ErrorReportRequest;
import org.triplea.http.client.error.report.ErrorReportResponse;
import org.triplea.http.client.github.GithubApiClient;
import org.triplea.http.client.lib.HttpClientHeaders;
import org.triplea.server.error.reporting.upload.CanUploadErrorReportStrategy;
import org.triplea.server.error.reporting.upload.CreateIssueParams;
import org.triplea.server.error.reporting.upload.ErrorReportModule;

/** Http controller that binds the error upload endpoint with the error report upload handler. */
@Path("/")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Builder
public class ErrorReportController {
  @Nonnull private final ErrorReportModule errorReportIngestion;
  @Nonnull private final Function<CanUploadRequest, CanUploadErrorReportResponse> canReportModule;

  /** Factory method. */
  public static ErrorReportController build(GithubApiClient githubApiClient, Jdbi jdbi) {
    return ErrorReportController.builder()
        .errorReportIngestion(ErrorReportModule.build(githubApiClient, jdbi))
        .canReportModule(CanUploadErrorReportStrategy.build(jdbi))
        .build();
  }

  @POST
  @Path(ErrorReportClient.CAN_UPLOAD_ERROR_REPORT_PATH)
  public CanUploadErrorReportResponse canUploadErrorReport(CanUploadRequest canUploadRequest) {
    if (canUploadRequest == null
        || canUploadRequest.getErrorTitle() == null
        || canUploadRequest.getGameVersion() == null) {
      throw new IllegalArgumentException("Missing request attributes title or game version");
    }

    return canReportModule.apply(canUploadRequest);
  }

  /**
   * Endpoint where users can submit an error report, the server will use an API token of a generic
   * user to in turn create a GitHub issue using the data from the error report.
   */
  @POST
  @Path(ErrorReportClient.ERROR_REPORT_PATH)
  public ErrorReportResponse uploadErrorReport(
      @Context HttpServletRequest request, ErrorReportRequest errorReport) {

    if (errorReport == null
        || errorReport.getBody() == null
        || errorReport.getTitle() == null
        || errorReport.getGameVersion() == null) {
      throw new IllegalArgumentException("Missing attribute, body, title, or game version");
    }

    return errorReportIngestion.createErrorReport(
        CreateIssueParams.builder()
            .ip(IpAddressExtractor.extractIpAddress(request))
            .systemId(request.getHeader(HttpClientHeaders.VERSION_HEADER))
            .errorReportRequest(errorReport)
            .build());
  }
}
