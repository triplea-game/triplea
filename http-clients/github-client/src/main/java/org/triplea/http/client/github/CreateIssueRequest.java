package org.triplea.http.client.github;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.triplea.http.client.HttpClientConstants;
import org.triplea.java.StringUtils;

/** Represents request data to create a github issue. */
@ToString
@EqualsAndHashCode
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateIssueRequest {
  private String title;
  private String body;
  private String[] labels;

  public String getTitle() {
    return title == null ? null : StringUtils.truncate(title, HttpClientConstants.TITLE_MAX_LENGTH);
  }

  public String getBody() {
    return body == null
        ? null
        : StringUtils.truncate(body, HttpClientConstants.REPORT_BODY_MAX_LENGTH);
  }
}
