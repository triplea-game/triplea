package org.triplea.debug.error.reporting;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import feign.FeignException;
import java.net.URI;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.http.client.SystemIdHeader;
import org.triplea.http.client.error.report.ErrorReportClient;
import org.triplea.http.client.error.report.ErrorReportRequest;
import org.triplea.http.client.error.report.ErrorReportResponse;

@ExtendWith(MockitoExtension.class)
class ErrorReportUploadActionTest {

  private static final ErrorReportRequest ERROR_REPORT =
      ErrorReportRequest.builder()
          .title("Extums prarere in audax tornacum!")
          .body("Rector de barbatus gemna, desiderium candidatus!")
          .build();

  private static final ErrorReportResponse SUCCESS_RESPONSE =
      ErrorReportResponse.builder().githubIssueLink("http://successful.send").build();

  @Mock private ErrorReportClient errorReportClient;
  @Mock private Consumer<URI> successConfirmation;
  @Mock private Consumer<FeignException> failureConfirmation;

  @Mock private FeignException feignException;

  private ErrorReportUploadAction errorReportUploadAction;

  @BeforeEach
  void setup() {
    errorReportUploadAction =
        ErrorReportUploadAction.builder()
            .serviceClient(errorReportClient)
            .failureConfirmation(failureConfirmation)
            .successConfirmation(successConfirmation)
            .build();
  }

  @Test
  void failureResponse() {
    when(errorReportClient.uploadErrorReport(SystemIdHeader.headers(), ERROR_REPORT))
        .thenThrow(feignException);

    final boolean result = errorReportUploadAction.test(ERROR_REPORT);

    assertThat(result, is(false));

    verify(failureConfirmation).accept(feignException);
    verify(successConfirmation, never()).accept(any());
  }

  @Test
  void successCase() {
    when(errorReportClient.uploadErrorReport(SystemIdHeader.headers(), ERROR_REPORT))
        .thenReturn(SUCCESS_RESPONSE);

    final boolean result = errorReportUploadAction.test(ERROR_REPORT);

    assertThat(result, is(true));

    verify(failureConfirmation, never()).accept(any());
    verify(successConfirmation).accept(URI.create(SUCCESS_RESPONSE.getGithubIssueLink()));
  }
}
