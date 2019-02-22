package org.triplea.server.reporting.error;

import java.util.function.Function;
import java.util.function.Predicate;

import javax.annotation.Nonnull;

import org.triplea.http.client.ServiceClient;
import org.triplea.http.client.ServiceResponse;
import org.triplea.http.client.error.report.create.ErrorReport;
import org.triplea.http.client.error.report.create.ErrorReportResponse;
import org.triplea.http.client.github.issues.create.CreateIssueResponse;

import lombok.Builder;

/** Performs the steps for uploading an error report from the point of view of the server. */
@Builder
public class ErrorUploadStrategy implements Function<ErrorReportRequest, ErrorReportResponse> {

  @Nonnull
  private final Function<ServiceResponse<CreateIssueResponse>, ErrorReportResponse> responseAdapter;
  @Nonnull
  private final ServiceClient<ErrorReport, CreateIssueResponse> createIssueClient;
  @Nonnull
  private final Predicate<ErrorReportRequest> allowErrorReport;

  @Override
  public ErrorReportResponse apply(final ErrorReportRequest errorReportRequest) {
    if (allowErrorReport.test(errorReportRequest)) {
      final ServiceResponse<CreateIssueResponse> response =
          createIssueClient.apply(errorReportRequest.getErrorReport());
      return responseAdapter.apply(response);
    }

    return ErrorReportResponse.builder()
        .error("Too many requests, please wait before submitting another request")
        .build();
  }
}
