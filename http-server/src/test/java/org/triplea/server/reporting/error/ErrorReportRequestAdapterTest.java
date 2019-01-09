package org.triplea.server.reporting.error;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringContains.containsString;

import org.junit.jupiter.api.Test;
import org.triplea.http.client.error.report.create.ErrorReport;
import org.triplea.http.client.github.issues.create.CreateIssueRequest;

class ErrorReportRequestAdapterTest {

  private static final ErrorReportRequest request =
      ErrorReportRequest.builder()
          .clientIp("")
          .errorReport(
              ErrorReport.builder()
                  .gameVersion("Fraticinida de rusticus abnoba, reperire adelphis!")
                  .javaVersion("Velox valebats ducunt ad tata.")
                  .operatingSystem("version")
                  .reportMessage("Never scrape a dubloon.")
                  .build())
          .build();

  @Test
  void apply() {
    final ErrorReportRequestAdapter errorReportRequestAdapter = new ErrorReportRequestAdapter();

    final CreateIssueRequest result = errorReportRequestAdapter.apply(request);

    asList(request.getErrorReport().getJavaVersion(), request.getErrorReport().getReportMessage())
        .forEach(expectedValue -> assertThat(result.getBody(), containsString(expectedValue)));
  }
}
