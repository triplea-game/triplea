package org.triplea.server.reporting.error.upload;

import java.util.function.Function;

import javax.annotation.Nonnull;

import org.triplea.http.client.ServiceClient;
import org.triplea.http.client.ServiceResponse;
import org.triplea.http.client.error.report.create.ErrorReport;
import org.triplea.http.client.error.report.create.ErrorReportResponse;
import org.triplea.http.client.github.issues.create.CreateIssueRequest;
import org.triplea.http.client.github.issues.create.CreateIssueResponse;

import lombok.Builder;

/**
 * Performs the steps for uploading an error report from the point of view of the server.
 */
@Builder
public class ErrorUploadStrategy implements Function<ErrorReport, ErrorReportResponse> {
  @Nonnull
  private final Function<ErrorReport, CreateIssueRequest> requestAdapter;
  @Nonnull
  private final Function<ServiceResponse<CreateIssueResponse>, ErrorReportResponse> responseAdapter;
  @Nonnull
  private final ServiceClient<CreateIssueRequest, CreateIssueResponse> createIssueClient;

  @Override
  public ErrorReportResponse apply(final ErrorReport errorReport) {
    final CreateIssueRequest createIssueRequest = requestAdapter.apply(errorReport);
    // TODO: have we done the appropriate filtering to not create bad requests?
    final ServiceResponse<CreateIssueResponse> response = createIssueClient.apply(createIssueRequest);
    return responseAdapter.apply(response);
  }
}
