package org.triplea.server.reporting.error;

import java.util.function.Function;

import org.triplea.http.client.ServiceResponse;
import org.triplea.http.client.error.report.create.ErrorReportResponse;
import org.triplea.http.client.github.issues.create.CreateIssueResponse;

class ErrorReportResponseAdapter
    implements Function<ServiceResponse<CreateIssueResponse>, ErrorReportResponse> {

  @Override
  public ErrorReportResponse apply(final ServiceResponse<CreateIssueResponse> response) {
    return ErrorReportResponse.builder()
        .error(response.getThrown()
            .map(Throwable::getMessage)
            .orElse(""))
        .githubIssueLink(response.getPayload()
            .map(CreateIssueResponse::getHtmlUrl)
            .orElse(""))
        .build();
  }
}
