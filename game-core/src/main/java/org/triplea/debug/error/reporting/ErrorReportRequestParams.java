package org.triplea.debug.error.reporting;

import java.util.logging.LogRecord;
import javax.annotation.Nonnull;
import lombok.Builder;
import lombok.Value;

/** Value object representing data gathered from user and an underlying error. */
@Value
@Builder
class ErrorReportRequestParams {
  @Nonnull private final String userDescription;
  @Nonnull private final String mapName;
  @Nonnull private final LogRecord logRecord;
}
