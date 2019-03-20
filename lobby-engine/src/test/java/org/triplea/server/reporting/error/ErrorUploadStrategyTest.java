package org.triplea.server.reporting.error;

import static com.github.npathai.hamcrestopt.OptionalMatchers.isEmpty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.mockito.Mockito.when;

import java.util.function.Function;
import java.util.function.Predicate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.http.client.ServiceClient;
import org.triplea.http.client.ServiceResponse;
import org.triplea.http.client.error.report.ErrorUploadRequest;
import org.triplea.http.client.error.report.ErrorUploadResponse;
import org.triplea.http.client.github.issues.create.CreateIssueResponse;

@ExtendWith(MockitoExtension.class)
class ErrorUploadStrategyTest {
  private static final ErrorUploadRequest ERROR_REPORT = ErrorUploadRequest.builder()
      .title("Decors volare in amivadum!")
      .body("Brabeuta camerarius imber est.")
      .build();
  private static final ErrorReportRequest ERROR_REPORT_REQUEST = ErrorReportRequest.builder()
      .clientIp("Nunquam imperium luna.")
      .errorReport(ERROR_REPORT)
      .build();

  @Mock
  private ServiceClient<ErrorUploadRequest, CreateIssueResponse> serviceClient;
  @Mock
  private ServiceResponse<CreateIssueResponse> serviceResponse;
  @Mock
  private ErrorUploadResponse errorReportResponse;
  @Mock
  private Function<ServiceResponse<CreateIssueResponse>, ErrorUploadResponse> responseAdapter;
  @Mock
  private Predicate<ErrorReportRequest> allowErrorReport;


  private ErrorUploadStrategy errorUploadStrategy;


  @BeforeEach
  void setup() {
    errorUploadStrategy = ErrorUploadStrategy.builder()
        .responseAdapter(responseAdapter)
        .createIssueClient(serviceClient)
        .allowErrorReport(allowErrorReport)
        .build();
  }

  @Test
  void apply() {
    when(allowErrorReport.test(ERROR_REPORT_REQUEST)).thenReturn(true);
    when(responseAdapter.apply(serviceResponse)).thenReturn(errorReportResponse);
    when(serviceClient.apply(ERROR_REPORT)).thenReturn(serviceResponse);

    final ErrorUploadResponse response = errorUploadStrategy.apply(ERROR_REPORT_REQUEST);

    assertThat(response, sameInstance(errorReportResponse));
  }


  @Test
  void filterRejectsRequest() {
    when(allowErrorReport.test(ERROR_REPORT_REQUEST)).thenReturn(false);

    final ErrorUploadResponse response = errorUploadStrategy.apply(ERROR_REPORT_REQUEST);

    assertThat(response.getError(), not(emptyString()));
    assertThat(response.getGithubIssueLink(), isEmpty());
  }
}
