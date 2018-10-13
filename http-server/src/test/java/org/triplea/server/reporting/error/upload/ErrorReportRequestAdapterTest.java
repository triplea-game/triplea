package org.triplea.server.reporting.error.upload;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringContains.containsString;

import org.junit.jupiter.api.Test;
import org.triplea.http.client.error.report.create.ErrorReport;
import org.triplea.http.client.error.report.create.ErrorReportDetails;
import org.triplea.http.client.github.issues.create.CreateIssueRequest;

class ErrorReportRequestAdapterTest {

  private static final ErrorReport errorReport = new ErrorReport(ErrorReportDetails.builder()
      .title("Fraticinida de rusticus abnoba, reperire adelphis!")
      .description("Velox valebats ducunt ad tata.")
      .gameVersion("version")
      .build());

  @Test
  void apply() {
    final ErrorReportRequestAdapter errorReportRequestAdapter = new ErrorReportRequestAdapter();

    final CreateIssueRequest result = errorReportRequestAdapter.apply(errorReport);

    asList(
        errorReport.getJavaVersion(),
        errorReport.getDescription())
            .forEach(expectedValue -> assertThat(
                result.getBody(),
                containsString(expectedValue)));

    assertThat(result.getTitle(), is(errorReport.getTitle()));

  }
}
