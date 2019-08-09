package org.triplea.http.client.moderator.toolbox.event.log;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.triplea.http.client.HttpClientTesting.API_KEY_PASSWORD;
import static org.triplea.http.client.HttpClientTesting.PAGING_PARAMS;
import static org.triplea.http.client.HttpClientTesting.toJson;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import java.net.URI;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.triplea.http.client.HttpClientTesting;
import org.triplea.http.client.moderator.toolbox.ToolboxHttpHeaders;
import ru.lanwen.wiremock.ext.WiremockResolver;
import ru.lanwen.wiremock.ext.WiremockUriResolver;

@ExtendWith({WiremockResolver.class, WiremockUriResolver.class})
class ToolboxEventLogClientTest {

  private static final ModeratorEvent MODERATOR_EVENT =
      ModeratorEvent.builder()
          .actionTarget("Death is a scrawny shark.")
          .date(Instant.now())
          .moderatorAction("Peglegs are the ales of the rainy love.")
          .moderatorName("Ah, weird death!")
          .build();

  private static ToolboxEventLogClient newClient(final WireMockServer wireMockServer) {
    final URI hostUri = URI.create(wireMockServer.url(""));
    return ToolboxEventLogClient.newClient(hostUri, API_KEY_PASSWORD);
  }

  @Test
  void lookupModeratorEvents(@WiremockResolver.Wiremock final WireMockServer server) {
    server.stubFor(
        WireMock.post(ToolboxEventLogClient.AUDIT_HISTORY_PATH)
            .withHeader(ToolboxHttpHeaders.API_KEY_HEADER, equalTo(API_KEY_PASSWORD.getApiKey()))
            .withHeader(
                ToolboxHttpHeaders.API_KEY_PASSWORD_HEADER, equalTo(API_KEY_PASSWORD.getPassword()))
            .withRequestBody(equalToJson(toJson(PAGING_PARAMS)))
            .willReturn(
                WireMock.aResponse()
                    .withStatus(200)
                    .withBody(
                        HttpClientTesting.toJson(Collections.singletonList(MODERATOR_EVENT)))));

    final List<ModeratorEvent> results = newClient(server).lookupModeratorEvents(PAGING_PARAMS);

    assertThat(results, hasSize(1));
    assertThat(results.get(0), is(MODERATOR_EVENT));
  }
}
