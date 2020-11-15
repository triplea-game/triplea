package org.triplea.http.client.error.report;

import com.google.common.base.Ascii;
import javax.annotation.Nonnull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.triplea.http.client.HttpClientConstants;

/** Represents data that would be uploaded to a server. */
@ToString
@EqualsAndHashCode
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorReportRequest {
  @Nonnull private String title;
  @Nonnull private String body;
  @Nonnull @Getter private String gameVersion;

  public String getTitle() {
    return Ascii.truncate(title, HttpClientConstants.TITLE_MAX_LENGTH, "...");
  }

  public String getBody() {
    return Ascii.truncate(body, HttpClientConstants.REPORT_BODY_MAX_LENGTH, "...");
  }
}
