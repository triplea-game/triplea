package org.triplea.http.client.latest.version;

import javax.annotation.Nonnull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class LatestVersionResponse {
  @Nonnull private final String latestEngineVersion;
}
