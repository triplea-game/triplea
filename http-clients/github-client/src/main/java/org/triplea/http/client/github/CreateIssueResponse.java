package org.triplea.http.client.github;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

/** Response JSON object from github after we create a new issue. */
@ToString
@AllArgsConstructor
@Getter
public class CreateIssueResponse {
  @SerializedName("html_url")
  private final String htmlUrl;
}
