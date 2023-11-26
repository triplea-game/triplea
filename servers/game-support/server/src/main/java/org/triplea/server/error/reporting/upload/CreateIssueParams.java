package org.triplea.server.error.reporting.upload;

import javax.annotation.Nonnull;
import lombok.Builder;
import lombok.Value;
import org.triplea.http.client.error.report.ErrorReportRequest;

@Value
@Builder(toBuilder = true)
public class CreateIssueParams {
  @Nonnull private final String ip;
  @Nonnull private final String systemId;
  @Nonnull private final ErrorReportRequest errorReportRequest;
}
