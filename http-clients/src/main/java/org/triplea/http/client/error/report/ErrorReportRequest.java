package org.triplea.http.client.error.report;

import com.google.common.base.Ascii;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.triplea.http.client.github.issues.GithubIssueClient;

/** Represents data that would be uploaded to a server. */
@ToString
@EqualsAndHashCode
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorReportRequest {
  private String title;
  private String body;
  @Getter private String gameVersion;

  public String getTitle() {
    return title == null ? null : Ascii.truncate(title, GithubIssueClient.TITLE_MAX_LENGTH, "...");
  }

  public String getBody() {
    return body == null
        ? null
        : Ascii.truncate(body, GithubIssueClient.REPORT_BODY_MAX_LENGTH, "...");
  }
}
