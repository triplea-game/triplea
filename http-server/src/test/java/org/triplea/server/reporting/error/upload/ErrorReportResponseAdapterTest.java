package org.triplea.server.reporting.error.upload;

import static com.github.npathai.hamcrestopt.OptionalMatchers.isEmpty;
import static com.github.npathai.hamcrestopt.OptionalMatchers.isPresentAndIs;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.text.IsEmptyString.isEmptyString;

import java.net.URI;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.triplea.http.client.SendResult;
import org.triplea.http.client.ServiceResponse;
import org.triplea.http.client.error.report.create.ErrorReportResponse;
import org.triplea.http.client.github.issues.create.CreateIssueResponse;

class ErrorReportResponseAdapterTest {

  private static final RuntimeException sampleException =
      new RuntimeException("Omnes planetaes quaestio nobilis, fatalis calceuses.");

  private static final ServiceResponse<CreateIssueResponse> ERROR_RESPONSE =
      ServiceResponse.<CreateIssueResponse>builder()
          .thrown(sampleException)
          .sendResult(SendResult.SERVER_ERROR)
          .build();

  private static final CreateIssueResponse CREATE_ISSUE_RESPONSE = new CreateIssueResponse("http://html_url");

  private static final ServiceResponse<CreateIssueResponse> SERVICE_RESPONSE =
      ServiceResponse.<CreateIssueResponse>builder()
          .payload(CREATE_ISSUE_RESPONSE)
          .sendResult(SendResult.SENT)
          .build();

  private ErrorReportResponseAdapter errorReportResponseAdapter;


  @BeforeEach
  void setup() {
    errorReportResponseAdapter = new ErrorReportResponseAdapter();
  }

  @Test
  void apply() {
    final ErrorReportResponse errorReportResponse = errorReportResponseAdapter.apply(SERVICE_RESPONSE);

    assertThat(
        errorReportResponse.getGithubIssueLink(),
        isPresentAndIs(URI.create(CREATE_ISSUE_RESPONSE.getHtmlUrl())));
    assertThat(errorReportResponse.getError(), isEmptyString());

  }

  @Test
  void applyWithErrorInput() {
    final ErrorReportResponse errorReportResponse = errorReportResponseAdapter.apply(ERROR_RESPONSE);

    assertThat(errorReportResponse.getGithubIssueLink(), isEmpty());
    assertThat(errorReportResponse.getError(), is(sampleException.getMessage()));
  }
}
