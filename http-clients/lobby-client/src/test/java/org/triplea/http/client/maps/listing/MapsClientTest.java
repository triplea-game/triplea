package org.triplea.http.client.maps.listing;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.triplea.http.client.WireMockTest;
import org.triplea.test.common.JsonUtil;
import ru.lanwen.wiremock.ext.WiremockResolver;

class MapsClientTest extends WireMockTest {

  private static final List<MapDownloadItem> mapsListingResponse =
      List.of(
          MapDownloadItem.builder()
              .mapName("map-1")
              .description("description-1")
              .downloadUrl("http://download-url")
              .previewImageUrl("http://preview-url")
              .lastCommitDateEpochMilli(10L)
              .build(),
          MapDownloadItem.builder()
              .mapName("map-2")
              .description("description-2")
              .downloadUrl("http://download-url-2")
              .previewImageUrl("http://preview-url-2")
              .lastCommitDateEpochMilli(20L)
              .mapTags(
                  List.of(
                      MapTag.builder().displayOrder(1).name("tag-name").value("tag-value").build()))
              .build());

  private static MapsClient newClient(final WireMockServer wireMockServer) {
    return newClient(wireMockServer, MapsClient::new);
  }

  @Test
  void sendGameHostingRequest(@WiremockResolver.Wiremock final WireMockServer wireMockServer) {
    wireMockServer.stubFor(
        get(MapsClient.MAPS_LISTING_PATH)
            .willReturn(
                WireMock.aResponse()
                    .withStatus(200)
                    .withBody(JsonUtil.toJson(mapsListingResponse))));

    final var result = newClient(wireMockServer).fetchMapDownloads();

    assertThat(result, is(mapsListingResponse));
  }
}
