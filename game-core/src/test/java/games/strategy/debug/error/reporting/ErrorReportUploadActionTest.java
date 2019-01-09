package games.strategy.debug.error.reporting;

import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.stream.Collectors;

import javax.swing.JFrame;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.http.client.SendResult;
import org.triplea.http.client.ServiceClient;
import org.triplea.http.client.ServiceResponse;
import org.triplea.http.client.error.report.create.ErrorReport;
import org.triplea.http.client.error.report.create.ErrorReportResponse;

@ExtendWith(MockitoExtension.class)
class ErrorReportUploadActionTest {


  @Mock
  private ServiceClient<ErrorReport, ErrorReportResponse> serviceClient;

  @Mock
  private ConfirmationDialogController dialogController;

  @InjectMocks
  private ErrorReportUploadAction errorReportUploadAction;


  @Mock
  private JFrame jFrame;

  private static final UserErrorReport USER_ERROR_REPORT = UserErrorReport.builder()
      .build();


  @Test
  void nonSuccessSends() {
    withNonSuccessSendResults()
        .forEach(notSuccessfulSend -> {
          when(serviceClient.apply(USER_ERROR_REPORT.toErrorReport()))
              .thenReturn(notSuccessfulSend);

          errorReportUploadAction.accept(null, USER_ERROR_REPORT);

          verify(dialogController).showFailureConfirmation(notSuccessfulSend);

          reset(serviceClient, dialogController);
        });
  }

  private Collection<ServiceResponse<ErrorReportResponse>> withNonSuccessSendResults() {
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

  @Test
  void missingIssueLinkInPaylaod() {
    withMissingIssueLinkResult()
        .forEach(missingLinkResult -> {

          when(serviceClient.apply(USER_ERROR_REPORT.toErrorReport()))
              .thenReturn(missingLinkResult);

          errorReportUploadAction.accept(null, USER_ERROR_REPORT);

          verify(dialogController).showFailureConfirmation(missingLinkResult);

          reset(serviceClient, dialogController);
        });
  }

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

    when(serviceClient.apply(USER_ERROR_REPORT.toErrorReport()))
        .thenReturn(successCase);

    errorReportUploadAction.accept(jFrame, USER_ERROR_REPORT);

    verify(dialogController).showSuccessConfirmation(successCase.getPayload().get().getGithubIssueLink().get());
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
