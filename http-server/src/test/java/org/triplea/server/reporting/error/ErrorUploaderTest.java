package org.triplea.server.reporting.error;

import static com.github.npathai.hamcrestopt.OptionalMatchers.isEmpty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.function.Function;
import java.util.function.Predicate;

import org.hamcrest.core.IsSame;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.http.client.ServiceClient;
import org.triplea.http.client.ServiceResponse;
import org.triplea.http.client.error.report.create.ErrorReportResponse;
import org.triplea.http.client.github.issues.create.CreateIssueRequest;
import org.triplea.http.client.github.issues.create.CreateIssueResponse;

@ExtendWith(MockitoExtension.class)
class ErrorUploaderTest {

  private static final URI SAMPLE_URI = URI.create("https://example");

  @Mock
  private ServiceClient<CreateIssueRequest, CreateIssueResponse> serviceClient;
  @Mock
  private ErrorReportRequest errorReport;
  @Mock
  private CreateIssueRequest createIssueRequest;
  @Mock
  private ServiceResponse<CreateIssueResponse> serviceResponse;
  @Mock
  private ErrorReportResponse errorReportResponse;
  @Mock
  private Function<ErrorReportRequest, CreateIssueRequest> requestAdapter;
  @Mock
  private Function<ServiceResponse<CreateIssueResponse>, ErrorReportResponse> responseAdapter;
  @Mock
  private Predicate<ErrorReportRequest> allowErrorReport;


  private ErrorUploadStrategy errorUploader;


  @BeforeEach
  void setup() {
    errorUploader = ErrorUploadStrategy.builder()
        .responseAdapter(responseAdapter)
        .requestAdapter(requestAdapter)
        .hostUri(SAMPLE_URI)
        .createIssueClient(serviceClient)
        .allowErrorReport(allowErrorReport)
        .build();
  }

  @Test
  void apply() {
    when(requestAdapter.apply(errorReport)).thenReturn(createIssueRequest);
    when(allowErrorReport.test(errorReport)).thenReturn(true);
    when(serviceClient.apply(SAMPLE_URI, createIssueRequest)).thenReturn(serviceResponse);
    when(responseAdapter.apply(serviceResponse)).thenReturn(errorReportResponse);

    final ErrorReportResponse response = errorUploader.apply(errorReport);

    assertThat(response, IsSame.sameInstance(errorReportResponse));
  }


  @Test
  void filterRejectsRequest() {
    when(allowErrorReport.test(errorReport)).thenReturn(false);

    final ErrorReportResponse response = errorUploader.apply(errorReport);

    assertThat(response.getError(), not(emptyString()));
    assertThat(response.getGithubIssueLink(), isEmpty());
  }
}
