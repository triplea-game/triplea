package org.triplea.http.client.github;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

/** Response object from Github listing the details of an organization's repositories. */
@ToString
@AllArgsConstructor
@Getter
class RepoListingResponse {
  @SerializedName("html_url")
  String htmlUrl;
}
