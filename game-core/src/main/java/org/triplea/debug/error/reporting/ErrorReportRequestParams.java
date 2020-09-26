package org.triplea.debug.error.reporting;

import javax.annotation.Nonnull;
import lombok.Builder;
import lombok.Value;
import org.triplea.debug.LoggerRecord;
import org.triplea.debug.console.window.DebugUtils;

/** Value object representing data gathered from user and an underlying error. */
@Value
@Builder
class ErrorReportRequestParams {
  @Nonnull private final String userDescription;
  private final String mapName;
  @Builder.Default @Nonnull private final String memoryStatistics = DebugUtils.getMemory();
  @Nonnull private final LoggerRecord logRecord;
}
