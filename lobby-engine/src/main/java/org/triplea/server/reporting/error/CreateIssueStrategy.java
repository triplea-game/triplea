package org.triplea.server.reporting.error;

import java.util.function.Function;
import java.util.function.Predicate;

import javax.annotation.Nonnull;

import org.triplea.http.client.error.report.ErrorUploadResponse;
import org.triplea.http.client.github.issues.GithubIssueClient;
import org.triplea.http.client.github.issues.create.CreateIssueResponse;

import lombok.Builder;

/** Performs the steps for uploading an error report from the point of view of the server. */
@Builder
public class CreateIssueStrategy implements Function<ErrorReportRequest, ErrorUploadResponse> {

  @Nonnull
  private final Function<CreateIssueResponse, ErrorUploadResponse> responseAdapter;
  @Nonnull
  private final GithubIssueClient createIssueClient;
  @Nonnull
  private final Predicate<org.triplea.server.reporting.error.ErrorReportRequest> allowErrorReport;

  @Override
  public ErrorUploadResponse apply(final ErrorReportRequest errorReportRequest) {
    if (allowErrorReport.test(errorReportRequest)) {
      final CreateIssueResponse response =
          createIssueClient.newIssue(errorReportRequest.getErrorReport());
      return responseAdapter.apply(response);
    }

    throw new CreateErrorReportException("Too many requests, please wait before submitting another request");
  }
}
