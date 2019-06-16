package org.triplea.server.error.reporting;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.function.Function;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.http.client.error.report.ErrorUploadRequest;
import org.triplea.http.client.error.report.ErrorUploadResponse;
import org.triplea.http.client.github.issues.GithubIssueClient;
import org.triplea.http.client.github.issues.create.CreateIssueResponse;
import org.triplea.lobby.server.db.dao.ErrorReportingDao;

@ExtendWith(MockitoExtension.class)
class CreateIssueStrategyTest {

  private static final ErrorReportRequest ERROR_REPORT_REQUEST = ErrorReportRequest.builder()
      .errorReport(ErrorUploadRequest.builder()
          .body("body")
          .title("title")
          .build())
      .clientIp("ip")
      .build();

  private CreateIssueStrategy createIssueStrategy;

  @Mock
  private GithubIssueClient githubIssueClient;
  @Mock
  private ErrorUploadResponse errorUploadResponse;
  @Mock
  private CreateIssueResponse createIssueResponse;
  @Mock
  private Function<CreateIssueResponse, ErrorUploadResponse> responseAdapter;
  @Mock
  private ErrorReportingDao errorReportingDao;

  @Test
  void whenGithubServiceClientIsTestWillReturnStub() {
    createIssueStrategy = CreateIssueStrategy.builder()
        .githubIssueClient(githubIssueClient)
        .allowErrorReport(value -> true)
        .isProduction(false)
        .responseAdapter(value -> null)
        .errorReportingDao(errorReportingDao)
        .build();
    when(githubIssueClient.isTest()).thenReturn(true);

    final ErrorUploadResponse response = createIssueStrategy.apply(ERROR_REPORT_REQUEST);

    assertThat(
        response.getGithubIssueLink(),
        is(CreateIssueStrategy.STUBBED_RETURN_VALUE));

    verify(errorReportingDao).insertHistoryRecord(ERROR_REPORT_REQUEST.getClientIp());
    verify(errorReportingDao).purgeOld(any());
  }

  @Test
  void willThrowIfReportingLimitIsReached() {
    createIssueStrategy = CreateIssueStrategy.builder()
        .githubIssueClient(githubIssueClient)
        // key setting here is that allow error report is false
        .allowErrorReport(value -> false)
        .isProduction(false)
        .responseAdapter(value -> null)
        .errorReportingDao(errorReportingDao)
        .build();

    assertThrows(CreateErrorReportException.class, () -> createIssueStrategy.apply(ERROR_REPORT_REQUEST));

    verify(errorReportingDao, never()).insertHistoryRecord(any());
    verify(errorReportingDao, never()).purgeOld(any());
  }

  @Test
  void verifyHappyCaseFlow() {
    createIssueStrategy = CreateIssueStrategy.builder()
        .githubIssueClient(githubIssueClient)
        .allowErrorReport(value -> true)
        .isProduction(true)
        .responseAdapter(responseAdapter)
        .errorReportingDao(errorReportingDao)
        .build();

    when(githubIssueClient.isTest()).thenReturn(false);
    when(githubIssueClient.newIssue(ERROR_REPORT_REQUEST.getErrorReport()))
        .thenReturn(createIssueResponse);
    when(responseAdapter.apply(createIssueResponse)).thenReturn(errorUploadResponse);

    final ErrorUploadResponse response = createIssueStrategy.apply(ERROR_REPORT_REQUEST);
    assertThat(response, sameInstance(errorUploadResponse));

    verify(errorReportingDao).insertHistoryRecord(ERROR_REPORT_REQUEST.getClientIp());
    verify(errorReportingDao).purgeOld(any());
  }
}
