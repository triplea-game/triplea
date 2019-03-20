package org.triplea.server.reporting.error;

import javax.annotation.Nonnull;

import org.triplea.http.client.error.report.ErrorUploadRequest;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/** Data object combining error report JSON and IP address from the incoming http request. */
@Builder
@Getter
@EqualsAndHashCode
public class ErrorReportRequest {
  @Nonnull
  private final ErrorUploadRequest errorReport;
  @Nonnull
  private final String clientIp;
}
