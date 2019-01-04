package org.triplea.server.reporting.error.upload;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

import java.util.function.Function;

import org.hamcrest.core.IsSame;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.http.client.ServiceClient;
import org.triplea.http.client.ServiceResponse;
import org.triplea.http.client.error.report.create.ErrorReport;
import org.triplea.http.client.error.report.create.ErrorReportResponse;
import org.triplea.http.client.github.issues.create.CreateIssueRequest;
import org.triplea.http.client.github.issues.create.CreateIssueResponse;

@ExtendWith(MockitoExtension.class)
class ErrorUploaderTest {

  @Mock
  private ServiceClient<CreateIssueRequest, CreateIssueResponse> serviceClient;
  @Mock
  private ErrorReport errorReport;
  @Mock
  private CreateIssueRequest createIssueRequest;
  @Mock
  private ServiceResponse<CreateIssueResponse> serviceResponse;
  @Mock
  private ErrorReportResponse errorReportResponse;
  @Mock
  private Function<ErrorReport, CreateIssueRequest> requestAdapter;
  @Mock
  private Function<ServiceResponse<CreateIssueResponse>, ErrorReportResponse> responseAdapter;

  private ErrorUploadStrategy errorUploader;


  @BeforeEach
  void setup() {
    errorUploader = ErrorUploadStrategy.builder()
        .responseAdapter(responseAdapter)
        .requestAdapter(requestAdapter)
        .createIssueClient(serviceClient)
        .build();
  }

  @Test
  void apply() {
    when(requestAdapter.apply(errorReport)).thenReturn(createIssueRequest);
    when(serviceClient.apply(createIssueRequest)).thenReturn(serviceResponse);
    when(responseAdapter.apply(serviceResponse)).thenReturn(errorReportResponse);

    final ErrorReportResponse response = errorUploader.apply(errorReport);

    assertThat(response, IsSame.sameInstance(errorReportResponse));
  }
}
