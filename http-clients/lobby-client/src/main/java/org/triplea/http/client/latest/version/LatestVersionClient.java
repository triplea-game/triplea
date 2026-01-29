package org.triplea.http.client.latest.version;

import feign.RequestLine;
import java.net.URI;
import org.triplea.http.client.HttpClient;
import org.triplea.http.client.lobby.AuthenticationHeaders;

public interface LatestVersionClient {
  String LATEST_VERSION_PATH = "/support/latest-version";

  static LatestVersionClient newClient(final URI uri) {
    return HttpClient.newClient(
        LatestVersionClient.class, uri, AuthenticationHeaders.systemIdHeaders());
  }

  /**
   * Gets latest engine version from triplea server
   *
   * @throws feign.FeignException thrown if server unavailable or returns 500
   */
  @RequestLine("GET " + LatestVersionClient.LATEST_VERSION_PATH)
  LatestVersionResponse fetchLatestVersion();
}
