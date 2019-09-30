package org.triplea.http.client.error.report;

import com.google.common.base.Ascii;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/** Represents data that would be uploaded to a server. */
@ToString
@EqualsAndHashCode
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorReportRequest {
  private String title;
  private String body;

  public String getTitle() {
    return title == null ? null : Ascii.truncate(title, 125, "...");
  }

  public String getBody() {
    return body == null ? null : Ascii.truncate(body, 20000, "...");
  }
}
