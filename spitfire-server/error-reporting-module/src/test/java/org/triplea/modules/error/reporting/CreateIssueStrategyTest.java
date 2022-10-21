package org.triplea.modules.error.reporting;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
class CreateIssueStrategyTest {

  private static final ErrorReportRequest ERROR_REPORT_REQUEST =
      ErrorReportRequest.builder().body("body").title("title").gameVersion("version").build();
  private static final String IP = "127.0.1.10";
  private static final String SYSTEM_ID = "system-id";

  private static final CreateIssueResponse CREATE_ISSUE_RESPONSE = new CreateIssueResponse("created-issue-link");
  @Mock private GithubApiClient githubApiClient;
  @Mock private ErrorReportingDao errorReportingDao;

  @Test
  void verifyFlow() {
    final CreateIssueStrategy createIssueStrategy =
        CreateIssueStrategy.builder()
            .githubOrg("org")
            .githubRepo("repo")
            .githubApiClient(githubApiClient)
            .errorReportingDao(errorReportingDao)
            .build();

    when(githubApiClient.newIssue(eq("org"), eq("repo"), any())).thenReturn(CREATE_ISSUE_RESPONSE);

    final ErrorReportResponse response =
        createIssueStrategy.apply(
            CreateIssueParams.builder()
                .ip(IP)
                .systemId(SYSTEM_ID)
                .errorReportRequest(ERROR_REPORT_REQUEST)
                .build());

    assertThat(response.getGithubIssueLink(), is(CREATE_ISSUE_RESPONSE.getHtmlUrl()));

    verify(errorReportingDao)
        .insertHistoryRecord(
            InsertHistoryRecordParams.builder()
                .title(ERROR_REPORT_REQUEST.getTitle())
                .gameVersion(ERROR_REPORT_REQUEST.getGameVersion())
                .githubIssueLink(CREATE_ISSUE_RESPONSE.getHtmlUrl())
                .systemId(SYSTEM_ID)
                .ip(IP)
                .build());
    verify(errorReportingDao).purgeOld(any());
  }
}
