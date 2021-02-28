package org.triplea.http.client.lobby.moderator.toolbox.banned.name;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.triplea.http.client.HttpClientTesting.EXPECTED_API_KEY;
import static org.triplea.http.client.HttpClientTesting.serve200ForToolboxPostWithBody;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.triplea.http.client.AuthenticationHeaders;
import org.triplea.http.client.WireMockTest;
import org.triplea.test.common.JsonUtil;
import ru.lanwen.wiremock.ext.WiremockResolver;

class ToolboxUsernameBanClientTest extends WireMockTest {
  private static final String USERNAME = "Faith ho! pull to be robed.";

  private static final UsernameBanData BANNED_USERNAME_DATA =
      UsernameBanData.builder()
          .banDate(Instant.now().toEpochMilli())
          .bannedName("Cannons grow with halitosis!")
          .build();

  private static ToolboxUsernameBanClient newClient(final WireMockServer wireMockServer) {
    return newClient(wireMockServer, ToolboxUsernameBanClient::newClient);
  }

  @Test
  void removeUsernameBan(@WiremockResolver.Wiremock final WireMockServer server) {
    serve200ForToolboxPostWithBody(
        server, ToolboxUsernameBanClient.REMOVE_BANNED_USER_NAME_PATH, USERNAME);

    newClient(server).removeUsernameBan(USERNAME);
  }

  @Test
  void addUsernameBan(@WiremockResolver.Wiremock final WireMockServer server) {
    serve200ForToolboxPostWithBody(
        server, ToolboxUsernameBanClient.ADD_BANNED_USER_NAME_PATH, USERNAME);

    newClient(server).addUsernameBan(USERNAME);
  }

  @Test
  void getUsernameBans(@WiremockResolver.Wiremock final WireMockServer server) {
    server.stubFor(
        WireMock.get(ToolboxUsernameBanClient.GET_BANNED_USER_NAMES_PATH)
            .withHeader(AuthenticationHeaders.API_KEY_HEADER, equalTo(EXPECTED_API_KEY))
            .willReturn(
                WireMock.aResponse()
                    .withStatus(200)
                    .withBody(JsonUtil.toJson(List.of(BANNED_USERNAME_DATA)))));

    final List<UsernameBanData> results = newClient(server).getUsernameBans();

    assertThat(results, hasSize(1));
    assertThat(results.get(0), is(BANNED_USERNAME_DATA));
  }
}
