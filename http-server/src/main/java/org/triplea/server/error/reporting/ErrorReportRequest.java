package org.triplea.server.error.reporting;

import javax.annotation.Nonnull;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/** Data object combining error report JSON and IP address from the incoming http request. */
@Builder
@Getter
@EqualsAndHashCode
public class ErrorReportRequest {
  @Nonnull private final org.triplea.http.client.error.report.ErrorReportRequest errorReport;
  @Nonnull private final String clientIp;
}
