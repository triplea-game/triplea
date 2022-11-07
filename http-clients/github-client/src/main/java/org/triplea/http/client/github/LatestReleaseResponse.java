package org.triplea.http.client.github;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LatestReleaseResponse {
  @SerializedName("tag_name")
  String tagName;
}
