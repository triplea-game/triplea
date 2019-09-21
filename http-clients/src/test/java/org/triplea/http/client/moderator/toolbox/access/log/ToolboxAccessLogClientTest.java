package org.triplea.http.client.moderator.toolbox.access.log;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.triplea.http.client.HttpClientTesting.API_KEY;
import static org.triplea.http.client.HttpClientTesting.EXPECTED_API_KEY;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import java.net.URI;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import org.hamcrest.collection.IsCollectionWithSize;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.triplea.http.client.AuthenticationHeaders;
import org.triplea.http.client.HttpClientTesting;
import ru.lanwen.wiremock.ext.WiremockResolver;
import ru.lanwen.wiremock.ext.WiremockUriResolver;

@ExtendWith({WiremockResolver.class, WiremockUriResolver.class})
class ToolboxAccessLogClientTest {
  private static final AccessLogData ACCESS_LOG_DATA =
      AccessLogData.builder()
          .accessDate(Instant.now())
          .hashedMac("Dubloon of an old life, fight the yellow fever!")
          .ip("Haul me pants, ye jolly woodchuck!")
          .registered(true)
          .username("Plunders grow with fortune at the cloudy norman island!")
          .build();

  private static ToolboxAccessLogClient newClient(final WireMockServer wireMockServer) {
    final URI hostUri = URI.create(wireMockServer.url(""));
    return ToolboxAccessLogClient.newClient(hostUri, API_KEY);
  }

  @Test
  void sendErrorReportSuccessCase(@WiremockResolver.Wiremock final WireMockServer server) {
    server.stubFor(
        WireMock.post(ToolboxAccessLogClient.FETCH_ACCESS_LOG_PATH)
            .withHeader(AuthenticationHeaders.API_KEY_HEADER, equalTo(EXPECTED_API_KEY))
            .withRequestBody(equalToJson(HttpClientTesting.toJson(HttpClientTesting.PAGING_PARAMS)))
            .willReturn(
                WireMock.aResponse()
                    .withStatus(200)
                    .withBody(
                        HttpClientTesting.toJson(Collections.singletonList(ACCESS_LOG_DATA)))));

    final List<AccessLogData> results =
        newClient(server).getAccessLog(HttpClientTesting.PAGING_PARAMS);

    assertThat(results, IsCollectionWithSize.hasSize(1));
    assertThat(results.get(0), is(ACCESS_LOG_DATA));
  }
}
