package org.triplea.server.reporting.error;

import java.util.function.Function;
import java.util.function.Predicate;

import javax.annotation.Nonnull;

import org.triplea.http.client.ServiceClient;
import org.triplea.http.client.ServiceResponse;
import org.triplea.http.client.error.report.ErrorUploadRequest;
import org.triplea.http.client.error.report.ErrorUploadResponse;
import org.triplea.http.client.github.issues.create.CreateIssueResponse;

import lombok.Builder;

/** Performs the steps for uploading an error report from the point of view of the server. */
@Builder
public class ErrorUploadStrategy implements Function<ErrorReportRequest, ErrorUploadResponse> {

  @Nonnull
  private final Function<ServiceResponse<CreateIssueResponse>, ErrorUploadResponse> responseAdapter;
  @Nonnull
  private final ServiceClient<ErrorUploadRequest, CreateIssueResponse> createIssueClient;
  @Nonnull
  private final Predicate<ErrorReportRequest> allowErrorReport;

  @Override
  public ErrorUploadResponse apply(final ErrorReportRequest errorReportRequest) {
    if (allowErrorReport.test(errorReportRequest)) {
      final ServiceResponse<CreateIssueResponse> response =
          createIssueClient.apply(errorReportRequest.getErrorReport());
      return responseAdapter.apply(response);
    }

    return ErrorUploadResponse.builder()
        .error("Too many requests, please wait before submitting another request")
        .build();
  }
}
