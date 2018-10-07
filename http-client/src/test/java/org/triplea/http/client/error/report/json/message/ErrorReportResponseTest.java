package org.triplea.http.client.error.report.json.message;

import static com.github.npathai.hamcrestopt.OptionalMatchers.isEmpty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import org.junit.jupiter.api.Test;

class ErrorReportResponseTest {

  @Test
  public void successFlag() {
    assertThat(ErrorReportResponse.SUCCESS_RESPONSE.isSuccess(), is(true));
    assertThat(ErrorReportResponse.builder().build().isSuccess(), is(false));
  }


  @Test
  public void verifyErrorReportIdBehavior() {
    assertThat(ErrorReportResponse.builder().build().getSavedReportId(), isEmpty());
    assertThat(
        ErrorReportResponse.builder()
            .savedReportId("")
            .build()
            .getSavedReportId()
            .isPresent(),
        is(false));

    final String exampleValue = "non empty";
    assertThat(
        ErrorReportResponse.builder()
            .savedReportId(exampleValue)
            .build()
            .getSavedReportId()
            .orElseThrow(() -> new IllegalStateException("expected a value to be present")),
        is(exampleValue));
  }
}
