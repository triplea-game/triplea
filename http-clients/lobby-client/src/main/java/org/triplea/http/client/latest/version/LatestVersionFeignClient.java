package org.triplea.http.client.latest.version;

import feign.RequestLine;

interface LatestVersionFeignClient {
  @RequestLine("GET " + LatestVersionClient.LATEST_VERSION_PATH)
  LatestVersionResponse fetchLatestVersion();
}
