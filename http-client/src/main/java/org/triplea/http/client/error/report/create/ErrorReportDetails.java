package org.triplea.http.client.error.report.create;

import java.util.Optional;
import java.util.logging.LogRecord;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

/**
 * Data object for parameters used to construct an {@code ErrorReport}.
 */
@Builder
@Getter(AccessLevel.PACKAGE)
public final class ErrorReportDetails {
  @Nullable
  private final String title;
  @Nullable
  private final String problemDescription;
  @Nonnull
  private final String gameVersion;
  @Nullable
  private final LogRecord logRecord;

  Optional<LogRecord> getLogRecord() {
    return Optional.ofNullable(logRecord);
  }
}
