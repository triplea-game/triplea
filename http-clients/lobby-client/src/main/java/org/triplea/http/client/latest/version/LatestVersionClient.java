package org.triplea.http.client.latest.version;

import java.net.URI;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.triplea.http.client.AuthenticationHeaders;
import org.triplea.http.client.HttpClient;
import org.triplea.http.client.HttpInteractionException;

@Slf4j
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class LatestVersionClient {
  public static final String LATEST_VERSION_PATH = "/latest-version";

  private final LatestVersionFeignClient latestVersionFeignClient;

  public static LatestVersionClient newClient(final URI uri) {
    return new LatestVersionClient(new HttpClient<>(LatestVersionFeignClient.class, uri).get());
  }

  /**
   * Gets latest engine version from triplea server
   *
   * @throws HttpInteractionException thrown if server unavailable or returns 500
   */
  public LatestVersionResponse fetchLatestVersion() {
    final var latestVersionResponse =
        latestVersionFeignClient.fetchLatestVersion(AuthenticationHeaders.systemIdHeaders());

    log.info(
        "Server fetch returned latest engine version: {}",
        latestVersionResponse.getLatestEngineVersion());
    return latestVersionResponse;
  }
}
