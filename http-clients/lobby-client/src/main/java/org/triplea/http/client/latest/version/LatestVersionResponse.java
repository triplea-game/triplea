package org.triplea.http.client.latest.version;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class LatestVersionResponse {
  private final String latestEngineVersion;
}
