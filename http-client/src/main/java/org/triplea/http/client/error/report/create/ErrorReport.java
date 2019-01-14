package org.triplea.http.client.error.report.create;

import javax.annotation.Nonnull;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/** Represents data that would be uploaded to a server. */
@Getter
@ToString
@EqualsAndHashCode
@Builder
public class ErrorReport {
  @Nonnull
  private final String reportMessage;
  @Nonnull
  private final String gameVersion;
  @Nonnull
  private final String operatingSystem;
  @Nonnull
  private final String javaVersion;
}
