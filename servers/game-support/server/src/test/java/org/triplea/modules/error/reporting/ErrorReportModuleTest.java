package org.triplea.modules.error.reporting;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.http.client.error.report.ErrorReportRequest;
import org.triplea.http.client.error.report.ErrorReportResponse;
import org.triplea.http.client.github.CreateIssueRequest;
import org.triplea.http.client.github.CreateIssueResponse;
import org.triplea.http.client.github.GithubApiClient;
import org.triplea.server.error.reporting.upload.CreateIssueParams;
import org.triplea.server.error.reporting.upload.ErrorReportModule;
import org.triplea.server.error.reporting.upload.ErrorReportingDao;
import org.triplea.server.error.reporting.upload.InsertHistoryRecordParams;

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

  /**
   * Given an input to create an error report, we validate the parameters we send to github to
   * create an issue.
   */
  @Test
  void validateDataSentToGithub() {
    errorReportModule.createErrorReport(createIssueParams);

    verify(githubApiClient)
        .newIssue(
            CreateIssueRequest.builder()
                .body("body")
                .title("title")
                .labels(new String[] {"Error Report", "version"})
                .build());
  }

  /**
   * Given a variety of 'inputVersion' representing game versions, we validate the data we send to
   * github (notably checking the labels that we send).
   */
  @ParameterizedTest
  @MethodSource
  void validateLabelsDataSentToGithub(String inputVersion, String[] expectedLabels) {
    errorReportModule.createErrorReport(
        createIssueParams.toBuilder()
            .errorReportRequest(
                createIssueParams.getErrorReportRequest().toBuilder()
                    .gameVersion(inputVersion)
                    .build())
            .build());

    verify(githubApiClient)
        .newIssue(
            CreateIssueRequest.builder()
                .body("body")
                .title("title")
                .labels(expectedLabels)
                .build());
  }

  @SuppressWarnings("unused")
  static Stream<Arguments> validateLabelsDataSentToGithub() {
    return Stream.of(
        Arguments.of("version", new String[] {"Error Report", "version"}),
        Arguments.of("1.1", new String[] {"Error Report", "1.1"}),
        Arguments.of("2.1.1", new String[] {"Error Report", "2.1"}),
        Arguments.of("3.2.1.1", new String[] {"Error Report", "3.2"}),
        Arguments.of("4.0.1", new String[] {"Error Report", "4.0"}),
        Arguments.of("5.1+123", new String[] {"Error Report", "5.1"}),
        Arguments.of("6.0+abc", new String[] {"Error Report", "6.0"}));
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
