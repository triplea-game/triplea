package org.triplea.server.reporting.error;

import org.triplea.http.client.ServiceClient;
import org.triplea.http.client.error.report.create.ErrorReport;
import org.triplea.http.client.github.issues.GithubIssueClientFactory;
import org.triplea.http.client.github.issues.IssueClientParams;
import org.triplea.http.client.github.issues.create.CreateIssueResponse;

/** Class that handles object creation and dependencies for error upload. */
public final class ErrorUploadConfiguration {
  private ErrorUploadConfiguration() {}

  /**
   * Factory method for {@code ErrorUploader}.
   */
  public static ErrorUploadStrategy newErrorUploader(final IssueClientParams issueClientParams) {
    final ServiceClient<ErrorReport, CreateIssueResponse> createIssueClient =
        GithubIssueClientFactory.newGithubIssueCreator(issueClientParams);

    return ErrorUploadStrategy.builder()
        .createIssueClient(createIssueClient)
        .responseAdapter(new ErrorReportResponseAdapter())
        .allowErrorReport(new ErrorReportGateKeeper())
        .build();
  }
}
