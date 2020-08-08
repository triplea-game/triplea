package org.triplea.modules.error.reporting;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.triplea.http.client.github.issues.CreateIssueResponse;

class ErrorReportResponseConverterTest {

  private static final CreateIssueResponse CREATE_ISSUE_RESPONSE =
      new CreateIssueResponse("http://html_url");
  private static final CreateIssueResponse ERROR_RESPONSE = new CreateIssueResponse(null);

  private final ErrorReportResponseConverter errorReportResponseAdapter =
      new ErrorReportResponseConverter();

  @Test
  void apply() {
    assertThat(
        errorReportResponseAdapter.apply(CREATE_ISSUE_RESPONSE).getGithubIssueLink(),
        is(CREATE_ISSUE_RESPONSE.getHtmlUrl()));
  }

  @Test
  void applyWithErrorInput() {
    assertThrows(
        CreateErrorReportException.class, () -> errorReportResponseAdapter.apply(ERROR_RESPONSE));
  }
}
