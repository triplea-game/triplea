package org.triplea.server.reporting.error;

import java.util.function.Function;

import org.triplea.http.client.github.issues.create.CreateIssueRequest;

class ErrorReportRequestAdapter implements Function<ErrorReportRequest, CreateIssueRequest> {
  @Override
  public CreateIssueRequest apply(final ErrorReportRequest errorReportRequest) {
    return CreateIssueRequest.builder()
        .title(errorReportRequest.getErrorReport().getTitle())
        .body(errorReportRequest.getErrorReport().toString())
        .build();
  }
}
