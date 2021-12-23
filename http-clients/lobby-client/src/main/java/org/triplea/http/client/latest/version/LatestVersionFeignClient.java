package org.triplea.http.client.latest.version;

import feign.HeaderMap;
import feign.RequestLine;
import java.util.Map;

interface LatestVersionFeignClient {
  @RequestLine("GET " + LatestVersionClient.LATEST_VERSION_PATH)
  LatestVersionResponse fetchLatestVersion(@HeaderMap Map<String, Object> headers);
}
