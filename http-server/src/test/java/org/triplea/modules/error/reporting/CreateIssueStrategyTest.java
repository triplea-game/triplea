package org.triplea.modules.error.reporting;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.db.dao.error.reporting.ErrorReportingDao;
import org.triplea.db.dao.error.reporting.InsertHistoryRecordParams;
import org.triplea.http.client.error.report.ErrorReportRequest;
import org.triplea.http.client.error.report.ErrorReportResponse;
import org.triplea.http.client.github.issues.CreateIssueResponse;
import org.triplea.http.client.github.issues.GithubIssueClient;

@ExtendWith(MockitoExtension.class)
class CreateIssueStrategyTest {

  private static final ErrorReportRequest ERROR_REPORT_REQUEST =
      ErrorReportRequest.builder().body("body").title("title").gameVersion("version").build();
  private static final String IP = "127.0.1.10";
  private static final String SYSTEM_ID = "system-id";

  @Mock private GithubIssueClient githubIssueClient;
  @Mock private ErrorReportResponse errorReportResponse;
  @Mock private CreateIssueResponse createIssueResponse;
  @Mock private Function<CreateIssueResponse, ErrorReportResponse> responseAdapter;
  @Mock private ErrorReportingDao errorReportingDao;

  @Test
  void verifyFlow() {
    final CreateIssueStrategy createIssueStrategy =
        CreateIssueStrategy.builder()
            .githubIssueClient(githubIssueClient)
            .responseAdapter(responseAdapter)
            .errorReportingDao(errorReportingDao)
            .build();

    when(githubIssueClient.newIssue(ERROR_REPORT_REQUEST)).thenReturn(createIssueResponse);
    when(responseAdapter.apply(createIssueResponse)).thenReturn(errorReportResponse);

    final ErrorReportResponse response =
        createIssueStrategy.apply(
            CreateIssueParams.builder()
                .ip(IP)
                .systemId(SYSTEM_ID)
                .errorReportRequest(ERROR_REPORT_REQUEST)
                .build());

    assertThat(response, sameInstance(errorReportResponse));

    verify(errorReportingDao)
        .insertHistoryRecord(
            InsertHistoryRecordParams.builder()
                .title(ERROR_REPORT_REQUEST.getTitle())
                .gameVersion(ERROR_REPORT_REQUEST.getGameVersion())
                .githubIssueLink(errorReportResponse.getGithubIssueLink())
                .systemId(SYSTEM_ID)
                .ip(IP)
                .build());
    verify(errorReportingDao).purgeOld(any());
  }
}
