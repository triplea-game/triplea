package games.strategy.debug.error.reporting;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.http.client.error.report.ErrorUploadClient;
import org.triplea.http.client.error.report.ErrorUploadRequest;
import org.triplea.http.client.error.report.ErrorUploadResponse;

import feign.FeignException;

@ExtendWith(MockitoExtension.class)
class ErrorReportUploadActionTest {

  private static final ErrorUploadRequest ERROR_REPORT = ErrorUploadRequest.builder()
      .title("Extums prarere in audax tornacum!")
      .body("Rector de barbatus gemna, desiderium candidatus!")
      .build();

  private static final ErrorUploadResponse SUCCESS_RESPONSE = ErrorUploadResponse.builder()
      .githubIssueLink("http://successful.send")
      .build();

  @Mock
  private ErrorUploadClient errorUploadClient;
  @Mock
  private Consumer<URI> successConfirmation;
  @Mock
  private Consumer<FeignException> failureConfirmation;

  @Mock
  private FeignException feignException;


  private ErrorReportUploadAction errorReportUploadAction;

  @BeforeEach
  void setup() {
    errorReportUploadAction = ErrorReportUploadAction.builder()
        .serviceClient(errorUploadClient)
        .failureConfirmation(failureConfirmation)
        .successConfirmation(successConfirmation)
        .build();
  }

  @Test
  void failureResponse() {
    when(errorUploadClient.uploadErrorReport(ERROR_REPORT)).thenThrow(feignException);

    final boolean result = errorReportUploadAction.test(ERROR_REPORT);

    assertThat(result, is(false));

    verify(failureConfirmation).accept(feignException);
    verify(successConfirmation, never()).accept(any());
  }

  @Test
  void successCase() {
    when(errorUploadClient.uploadErrorReport(ERROR_REPORT)).thenReturn(SUCCESS_RESPONSE);

    final boolean result = errorReportUploadAction.test(ERROR_REPORT);

    assertThat(result, is(true));

    verify(failureConfirmation, never()).accept(any());
    verify(successConfirmation).accept(URI.create(SUCCESS_RESPONSE.getGithubIssueLink()));
  }
}
