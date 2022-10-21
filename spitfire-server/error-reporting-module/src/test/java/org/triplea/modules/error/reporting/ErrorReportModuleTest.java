package org.triplea.modules.error.reporting;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.http.client.error.report.ErrorReportRequest;
import org.triplea.http.client.error.report.ErrorReportResponse;
import org.triplea.http.client.github.CreateIssueResponse;
import org.triplea.http.client.github.GithubApiClient;
import org.triplea.modules.error.reporting.db.ErrorReportingDao;
import org.triplea.modules.error.reporting.db.InsertHistoryRecordParams;

@ExtendWith(MockitoExtension.class)
class ErrorReportModuleTest {

  private static final ErrorReportRequest ERROR_REPORT_REQUEST =
      ErrorReportRequest.builder().body("body").title("title").gameVersion("version").build();

  private static final String GITHUB_ISSUE_URL = "example-url-value";

  private static final CreateIssueParams createIssueParams =
      CreateIssueParams.builder()
          .ip("127.0.1.10")
          .systemId("system-id")
          .errorReportRequest(ERROR_REPORT_REQUEST)
          .build();

  @Mock private GithubApiClient githubApiClient;
  @Mock private ErrorReportingDao errorReportingDao;

  private ErrorReportModule errorReportModule;

  @BeforeEach
  void setup() {
    errorReportModule =
        ErrorReportModule.builder()
            .githubApiClient(githubApiClient)
            .errorReportingDao(errorReportingDao)
            .build();

    when(githubApiClient.newIssue(any())).thenReturn(new CreateIssueResponse(GITHUB_ISSUE_URL));
  }

  @Test
  void newIssueLinkIsReturnedToClient() {
    final ErrorReportResponse response = errorReportModule.createErrorReport(createIssueParams);

    assertThat(response.getGithubIssueLink(), is(GITHUB_ISSUE_URL));
  }

  @Test
  void errorReportIsLoggedToDatabase() {
    errorReportModule.createErrorReport(createIssueParams);

    verify(errorReportingDao)
        .insertHistoryRecord(
            InsertHistoryRecordParams.builder()
                .title(ERROR_REPORT_REQUEST.getTitle())
                .gameVersion(ERROR_REPORT_REQUEST.getGameVersion())
                .githubIssueLink(GITHUB_ISSUE_URL)
                .systemId(createIssueParams.getSystemId())
                .ip(createIssueParams.getIp())
                .build());
  }

  @Test
  void oldErrorReportsArePurged() {
    errorReportModule.createErrorReport(createIssueParams);

    verify(errorReportingDao).purgeOld(any());
  }
}
