package org.triplea.http.client.error.report;

import javax.annotation.Nonnull;

import com.google.common.base.Ascii;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/** Represents data that would be uploaded to a server. */
@ToString
@EqualsAndHashCode
@Builder
public class ErrorUploadRequest {
  @Nonnull
  private final String title;
  @Nonnull
  private final String body;

  public String getTitle() {
    return Ascii.truncate(title, 125, "...");
  }

  public String getBody() {
    return Ascii.truncate(body, 20000, "...");
  }
}
