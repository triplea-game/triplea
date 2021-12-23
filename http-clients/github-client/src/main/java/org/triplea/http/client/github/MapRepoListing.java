package org.triplea.http.client.github;

import com.google.gson.annotations.SerializedName;
import java.net.URI;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/** Response object from Github listing the details of an organization's repositories. */
@ToString
@AllArgsConstructor
@EqualsAndHashCode
@Builder
public class MapRepoListing {
  @SerializedName("html_url")
  String htmlUrl;

  @Getter String name;

  public URI getUri() {
    return URI.create(htmlUrl);
  }
}
