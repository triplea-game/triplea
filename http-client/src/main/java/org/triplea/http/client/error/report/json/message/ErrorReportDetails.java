package org.triplea.http.client.error.report.json.message;

import java.util.Optional;
import java.util.logging.LogRecord;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.base.Strings;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

/**
 * Data object for parameters used to construct an {@code ErrorReport}.
 */
@Builder
@Getter(AccessLevel.PACKAGE)
public final class ErrorReportDetails {
  @Nonnull
  private final String gameVersion;
  @Nullable
  private final String messageFromUser;
  @Nullable
  private final LogRecord logRecord;

  String getMessageFromUser() {
    return Strings.nullToEmpty(messageFromUser);
  }

  Optional<LogRecord> getLogRecord() {
    return Optional.ofNullable(logRecord);
  }
}
