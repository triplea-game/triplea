package org.triplea.http.client.latest.version;

import javax.annotation.Nonnull;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LatestVersionResponse {
  @Nonnull private final String latestEngineVersion;
  @Nonnull private final String releaseNotesUrl;
  @Nonnull private final String downloadUrl;
}
