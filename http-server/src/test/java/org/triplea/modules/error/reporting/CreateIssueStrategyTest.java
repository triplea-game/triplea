package org.triplea.modules.error.reporting;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.db.dao.ErrorReportingDao;
import org.triplea.http.client.error.report.ErrorReportRequest;
import org.triplea.http.client.error.report.ErrorReportResponse;
import org.triplea.http.client.github.issues.GithubIssueClient;
import org.triplea.http.client.github.issues.create.CreateIssueResponse;

@ExtendWith(MockitoExtension.class)
class CreateIssueStrategyTest {

  private static final ErrorReportRequest ERROR_REPORT_REQUEST =
      ErrorReportRequest.builder().body("body").title("title").build();
  private static final String IP = "127.0.1.10";

  private CreateIssueStrategy createIssueStrategy;

  @Mock private GithubIssueClient githubIssueClient;
  @Mock private ErrorReportResponse errorReportResponse;
  @Mock private CreateIssueResponse createIssueResponse;
  @Mock private Function<CreateIssueResponse, ErrorReportResponse> responseAdapter;
  @Mock private ErrorReportingDao errorReportingDao;

  @Test
  void whenGithubServiceClientIsTestWillReturnStub() {
    createIssueStrategy =
        CreateIssueStrategy.builder()
            .githubIssueClient(githubIssueClient)
            .responseAdapter(value -> null)
            .errorReportingDao(errorReportingDao)
            .build();
    when(githubIssueClient.isTest()).thenReturn(true);

    final ErrorReportResponse response = createIssueStrategy.apply(IP, ERROR_REPORT_REQUEST);

    assertThat(response.getGithubIssueLink(), is(CreateIssueStrategy.STUBBED_RETURN_VALUE));

    verify(errorReportingDao).insertHistoryRecord(IP);
    verify(errorReportingDao).purgeOld(any());
  }

  @Test
  void verifyHappyCaseFlow() {
    createIssueStrategy =
        CreateIssueStrategy.builder()
            .githubIssueClient(githubIssueClient)
            .responseAdapter(responseAdapter)
            .errorReportingDao(errorReportingDao)
            .build();

    when(githubIssueClient.isTest()).thenReturn(false);
    when(githubIssueClient.newIssue(ERROR_REPORT_REQUEST)).thenReturn(createIssueResponse);
    when(responseAdapter.apply(createIssueResponse)).thenReturn(errorReportResponse);

    final ErrorReportResponse response = createIssueStrategy.apply(IP, ERROR_REPORT_REQUEST);
    assertThat(response, sameInstance(errorReportResponse));

    verify(errorReportingDao).insertHistoryRecord(IP);
    verify(errorReportingDao).purgeOld(any());
  }
}
