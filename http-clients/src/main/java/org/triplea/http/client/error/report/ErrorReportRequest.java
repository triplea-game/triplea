package org.triplea.http.client.error.report;

import com.google.common.base.Ascii;
import javax.annotation.Nullable;
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
  @Nullable private String title;
  @Getter @Nullable private String body;

  public String getTitle() {
    return title == null ? null : Ascii.truncate(title, GithubIssueClient.TITLE_MAX_LENGTH, "...");
  }
}
