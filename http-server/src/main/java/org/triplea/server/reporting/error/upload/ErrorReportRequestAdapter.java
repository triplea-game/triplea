package org.triplea.server.reporting.error.upload;

import java.util.function.Function;

import org.triplea.http.client.error.report.create.ErrorReport;
import org.triplea.http.client.github.issues.create.CreateIssueRequest;

class ErrorReportRequestAdapter implements Function<ErrorReport, CreateIssueRequest> {
  @Override
  public CreateIssueRequest apply(final ErrorReport errorReport) {
    return CreateIssueRequest.builder()
        .title(errorReport.getTitle())
        .body(errorReport.toString())
        .build();
  }
}
