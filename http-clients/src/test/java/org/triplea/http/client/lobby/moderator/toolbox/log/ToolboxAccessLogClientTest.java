package org.triplea.http.client.lobby.moderator.toolbox.log;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.triplea.http.client.HttpClientTesting.EXPECTED_API_KEY;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import java.time.Instant;
import java.util.List;
import org.hamcrest.collection.IsCollectionWithSize;
import org.junit.jupiter.api.Test;
import org.triplea.http.client.AuthenticationHeaders;
import org.triplea.http.client.HttpClientTesting;
import org.triplea.http.client.WireMockTest;
import ru.lanwen.wiremock.ext.WiremockResolver;

class ToolboxAccessLogClientTest extends WireMockTest {
  private static final AccessLogData ACCESS_LOG_DATA =
      AccessLogData.builder()
          .accessDate(Instant.now().toEpochMilli())
          .systemId("Dubloon of an old life, fight the yellow fever!")
          .ip("Haul me pants, ye jolly woodchuck!")
          .registered(true)
          .username("Plunders grow with fortune at the cloudy norman island!")
          .build();

  private static ToolboxAccessLogClient newClient(final WireMockServer wireMockServer) {
    return newClient(wireMockServer, ToolboxAccessLogClient::newClient);
  }

  @Test
  void sendErrorReportSuccessCase(@WiremockResolver.Wiremock final WireMockServer server) {
    server.stubFor(
        WireMock.post(ToolboxAccessLogClient.FETCH_ACCESS_LOG_PATH)
            .withHeader(AuthenticationHeaders.API_KEY_HEADER, equalTo(EXPECTED_API_KEY))
            .withRequestBody(equalToJson(toJson(HttpClientTesting.PAGING_PARAMS)))
            .willReturn(
                WireMock.aResponse().withStatus(200).withBody(toJson(List.of(ACCESS_LOG_DATA)))));

    final List<AccessLogData> results =
        newClient(server).getAccessLog(HttpClientTesting.PAGING_PARAMS);

    assertThat(results, IsCollectionWithSize.hasSize(1));
    assertThat(results.get(0), is(ACCESS_LOG_DATA));
  }
}
