package org.triplea.server.reporting.error;

import java.util.function.Function;

import org.triplea.http.client.github.issues.create.CreateIssueRequest;

import com.google.common.base.Ascii;

class ErrorReportRequestAdapter implements Function<ErrorReportRequest, CreateIssueRequest> {
  @Override
  public CreateIssueRequest apply(final ErrorReportRequest errorReportRequest) {
    final String report = errorReportRequest.getErrorReport().toString();
    return CreateIssueRequest.builder()
        .title(Ascii.truncate(report, 125, "..."))
        .body(report)
        .build();
  }
}
