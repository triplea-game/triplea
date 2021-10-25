package org.triplea.http.client.latest.version;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.Test;
import org.triplea.http.client.HttpInteractionException;
import org.triplea.http.client.WireMockTest;
import org.triplea.test.common.JsonUtil;
import ru.lanwen.wiremock.ext.WiremockResolver;

class LatestVersionClientTest extends WireMockTest {
  private static LatestVersionClient newClient(final WireMockServer wireMockServer) {
    return newClient(wireMockServer, LatestVersionClient::newClient);
  }

  @Test
  void fetchLatestVersion_respondWith200(
      @WiremockResolver.Wiremock final WireMockServer wireMockServer) {
    wireMockServer.stubFor(
        get(LatestVersionClient.LATEST_VERSION_PATH)
            .willReturn(
                WireMock.aResponse()
                    .withStatus(200)
                    .withBody(
                        JsonUtil.toJson(
                            LatestVersionResponse.builder()
                                .latestEngineVersion("123.xyz")
                                .downloadUrl("download-url")
                                .releaseNotesUrl("release-notes-url")
                                .build()))));

    final LatestVersionResponse latestVersionResponse =
        newClient(wireMockServer).fetchLatestVersion();

    assertThat(latestVersionResponse.getLatestEngineVersion(), is("123.xyz"));
  }

  /** Server returns 500, client will throw HttpInteractionException */
  @Test
  void fetchLatestVersion_respondWith500(
      @WiremockResolver.Wiremock final WireMockServer wireMockServer) {
    wireMockServer.stubFor(
        get(LatestVersionClient.LATEST_VERSION_PATH)
            .willReturn(WireMock.aResponse().withStatus(500)));

    assertThrows(
        HttpInteractionException.class, () -> newClient(wireMockServer).fetchLatestVersion());
  }
}
