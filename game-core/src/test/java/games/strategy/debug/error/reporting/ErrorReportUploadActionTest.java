package games.strategy.debug.error.reporting;

import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.http.client.SendResult;
import org.triplea.http.client.ServiceClient;
import org.triplea.http.client.ServiceResponse;
import org.triplea.http.client.error.report.create.ErrorReport;
import org.triplea.http.client.error.report.create.ErrorReportResponse;

@ExtendWith(MockitoExtension.class)
class ErrorReportUploadActionTest {

  private static final ErrorReport ERROR_REPORT = ErrorReport.builder()
      .title("Extums prarere in audax tornacum!")
      .body("Rector de barbatus gemna, desiderium candidatus!")
      .build();

  @Mock
  private ServiceClient<ErrorReport, ErrorReportResponse> serviceClient;
  @Mock
  private Consumer<URI> successConfirmation;
  @Mock
  private Consumer<ServiceResponse<ErrorReportResponse>> failureConfirmation;

  private ErrorReportUploadAction errorReportUploadAction;

  @BeforeEach
  void setup() {
    errorReportUploadAction = ErrorReportUploadAction.builder()
        .serviceClient(serviceClient)
        .failureConfirmation(failureConfirmation)
        .successConfirmation(successConfirmation)
        .build();
  }

  @ParameterizedTest
  @MethodSource("withNonSuccessSendResults")
  void nonSuccessSends(final ServiceResponse<ErrorReportResponse> notSuccessfulSend) {
    when(serviceClient.apply(ERROR_REPORT)).thenReturn(notSuccessfulSend);

    final boolean result = errorReportUploadAction.test(ERROR_REPORT);

    assertThat(result, is(false));

    verify(failureConfirmation).accept(notSuccessfulSend);
    verify(successConfirmation, never()).accept(any());
  }

  @SuppressWarnings("unused") // Used by @MethodSource via reflection
  private static Collection<ServiceResponse<ErrorReportResponse>> withNonSuccessSendResults() {
    // Create ServiceResponses with each of the 'SendResult' values except for the successful SendResult value: 'SENT'
    return stream(SendResult.values())
        .filter(sendResult -> sendResult != SendResult.SENT)
        .map(ErrorReportUploadActionTest::serviceResponseWithSendResult)
        .collect(Collectors.toSet());
  }

  private static ServiceResponse<ErrorReportResponse> serviceResponseWithSendResult(final SendResult sendResult) {
    return ServiceResponse.<ErrorReportResponse>builder()
        .sendResult(sendResult)
        .build();
  }

  @ParameterizedTest
  @MethodSource("withMissingIssueLinkResult")
  void missingIssueLinkInPaylaod(final ServiceResponse<ErrorReportResponse> missingLinkResult) {
    when(serviceClient.apply(ERROR_REPORT)).thenReturn(missingLinkResult);

    final boolean result = errorReportUploadAction.test(ERROR_REPORT);

    assertThat(result, is(false));

    verify(failureConfirmation).accept(missingLinkResult);
    verify(successConfirmation, never()).accept(any());
  }

  @SuppressWarnings("unused") // Used by @MethodSource via reflection
  private static Collection<ServiceResponse<ErrorReportResponse>> withMissingIssueLinkResult() {
    return asList(
        ServiceResponse.<ErrorReportResponse>builder()
            .sendResult(SendResult.SENT)
            .build(),
        ServiceResponse.<ErrorReportResponse>builder()
            .sendResult(SendResult.SENT)
            .payload(ErrorReportResponse.builder()
                .build())
            .build(),
        ServiceResponse.<ErrorReportResponse>builder()
            .sendResult(SendResult.SENT)
            .payload(ErrorReportResponse.builder()
                .error("simulated error")
                .build())
            .build(),
        ServiceResponse.<ErrorReportResponse>builder()
            .sendResult(SendResult.SENT)
            .payload(ErrorReportResponse.builder()
                .githubIssueLink("")
                .build())
            .build());
  }

  @Test
  void successCase() {
    final ServiceResponse<ErrorReportResponse> successCase = withSuccessfulSend();
    when(serviceClient.apply(ERROR_REPORT)).thenReturn(successCase);

    final boolean result = errorReportUploadAction.test(ERROR_REPORT);

    assertThat(result, is(true));

    verify(failureConfirmation, never()).accept(any());
    verify(successConfirmation)
        .accept(successCase.getPayload().get().getGithubIssueLink().get());
  }

  private static ServiceResponse<ErrorReportResponse> withSuccessfulSend() {
    return ServiceResponse.<ErrorReportResponse>builder()
        .sendResult(SendResult.SENT)
        .payload(ErrorReportResponse.builder()
            .githubIssueLink("http://successful.send")
            .build())
        .build();
  }
}
