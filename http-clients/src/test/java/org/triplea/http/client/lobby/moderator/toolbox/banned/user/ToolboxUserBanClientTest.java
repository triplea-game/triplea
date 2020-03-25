package org.triplea.http.client.lobby.moderator.toolbox.banned.user;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.triplea.http.client.HttpClientTesting.EXPECTED_API_KEY;
import static org.triplea.http.client.HttpClientTesting.serve200ForToolboxPostWithBody;
import static org.triplea.http.client.HttpClientTesting.serve200ForToolboxPostWithBodyJson;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.triplea.http.client.AuthenticationHeaders;
import org.triplea.http.client.WireMockTest;
import ru.lanwen.wiremock.ext.WiremockResolver;

class ToolboxUserBanClientTest extends WireMockTest {

  private static final String BAN_ID = "Halitosis is a weird cannibal.";

  private static final UserBanData BANNED_USER_DATA =
      UserBanData.builder()
          .banDate(Instant.now())
          .banExpiry(Instant.now().plusSeconds(100))
          .banId("Yarr, sunny freebooter. you won't haul the bikini atoll.")
          .hashedMac("Crush me shark, ye undead dubloon!")
          .ip("Seashells whine with horror!")
          .username("The furner stutters urchin like a black fish.")
          .build();

  private static final UserBanParams BAN_USER_PARAMS =
      UserBanParams.builder()
          .systemId("Why does the skull grow?")
          .minutesToBan(15)
          .ip("Fall loudly like a jolly son.")
          .username("Grace is a scrawny breeze.")
          .build();

  private static ToolboxUserBanClient newClient(final WireMockServer wireMockServer) {
    return newClient(wireMockServer, ToolboxUserBanClient::newClient);
  }

  @Test
  void getUserBans(@WiremockResolver.Wiremock final WireMockServer server) {
    server.stubFor(
        WireMock.get(ToolboxUserBanClient.GET_USER_BANS_PATH)
            .withHeader(AuthenticationHeaders.API_KEY_HEADER, equalTo(EXPECTED_API_KEY))
            .willReturn(
                WireMock.aResponse().withStatus(200).withBody(toJson(List.of(BANNED_USER_DATA)))));

    final List<UserBanData> result = newClient(server).getUserBans();

    assertThat(result, hasSize(1));
    assertThat(result.get(0), is(BANNED_USER_DATA));
  }

  @Test
  void removeUserBan(@WiremockResolver.Wiremock final WireMockServer server) {
    serve200ForToolboxPostWithBody(server, ToolboxUserBanClient.REMOVE_USER_BAN_PATH, BAN_ID);

    newClient(server).removeUserBan(BAN_ID);
  }

  @Test
  void banUser(@WiremockResolver.Wiremock final WireMockServer server) {
    serve200ForToolboxPostWithBodyJson(server, ToolboxUserBanClient.BAN_USER_PATH, BAN_USER_PARAMS);

    newClient(server).banUser(BAN_USER_PARAMS);
  }
}
