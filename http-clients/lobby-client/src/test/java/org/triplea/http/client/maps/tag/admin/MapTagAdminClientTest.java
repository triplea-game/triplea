package org.triplea.http.client.maps.tag.admin;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.triplea.http.client.GenericServerResponse;
import org.triplea.http.client.WireMockTest;
import org.triplea.test.common.JsonUtil;
import ru.lanwen.wiremock.ext.WiremockResolver;

class MapTagAdminClientTest extends WireMockTest {

  private static MapTagAdminClient newClient(final WireMockServer wireMockServer) {
    return newClient(wireMockServer, MapTagAdminClient::newClient);
  }

  @Test
  void sendGetMapTagMetaDataRequest(
      @WiremockResolver.Wiremock final WireMockServer wireMockServer) {

    final var getMapTagMetaDataResponse =
        List.of(
            MapTagMetaData.builder()
                .tagName("tag1")
                .displayOrder(1)
                .allowedValues(List.of("", "value1", "value2"))
                .build(),
            MapTagMetaData.builder()
                .tagName("tag2")
                .displayOrder(2)
                .allowedValues(List.of("true", "false"))
                .build());

    wireMockServer.stubFor(
        get(MapTagAdminClient.GET_MAP_TAGS_META_DATA_PATH)
            .willReturn(
                WireMock.aResponse()
                    .withStatus(200)
                    .withBody(JsonUtil.toJson(getMapTagMetaDataResponse))));

    final var result = newClient(wireMockServer).fetchAllowedMapTagValues();

    assertThat(result, is(getMapTagMetaDataResponse));
  }

  @Test
  void sendUpdateMapTagRequest(@WiremockResolver.Wiremock final WireMockServer wireMockServer) {

    final var updateMapTagResponse = GenericServerResponse.SUCCESS;

    wireMockServer.stubFor(
        post(MapTagAdminClient.UPDATE_MAP_TAG_PATH)
            .willReturn(
                WireMock.aResponse()
                    .withStatus(200)
                    .withBody(JsonUtil.toJson(GenericServerResponse.SUCCESS))));

    final var result =
        newClient(wireMockServer)
            .updateMapTag(
                UpdateMapTagRequest.builder()
                    .mapName("map-name")
                    .tagName("tag-name")
                    .newTagValue("new-tag-value")
                    .build());

    assertThat(result, is(updateMapTagResponse));
  }
}
