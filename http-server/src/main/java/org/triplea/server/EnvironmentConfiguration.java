package org.triplea.server;

import java.net.URI;

import lombok.Builder;
import lombok.Getter;

/**
 * Stores values that will vary between test and prod.
 */
@Builder
@Getter
public class EnvironmentConfiguration {
  private final String githubAuthToken;
  private final String githubOrg;
  private final String githubRepo;
  private final URI githubHost;

  static EnvironmentConfiguration prod() {
    return builder()
        .githubHost(URI.create("https://api.github.com")) // "http://localhost:4567"))
        .githubOrg("triplea-game")
        // TODO: update test->triplea
        .githubRepo("test")
        // TODO: read the auth token from somewhere secret (env variable, file, s3)
        .githubAuthToken("")
        .build();
  }
}
