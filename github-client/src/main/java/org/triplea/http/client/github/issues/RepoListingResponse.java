package org.triplea.http.client.github.issues;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@ToString
@AllArgsConstructor
@Getter
class RepoListingResponse {
  @SerializedName("html_url")
  String htmlUrl;
}
