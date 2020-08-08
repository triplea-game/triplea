package org.triplea.http.client.lobby.moderator.toolbox.management;

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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.triplea.http.client.AuthenticationHeaders;
import org.triplea.http.client.WireMockTest;
import ru.lanwen.wiremock.ext.WiremockResolver;

@SuppressWarnings("InnerClassMayBeStatic")
class ToolboxModeratorManagementClientTest extends WireMockTest {

  private static final String MODERATOR_NAME = "Ooh! Pieces o' urchin are forever coal-black.";
  private static final ModeratorInfo MODERATOR_INFO =
      ModeratorInfo.builder()
          .name("Oh, power!")
          .lastLoginEpochMillis(Instant.now().toEpochMilli())
          .build();

  private static ToolboxModeratorManagementClient newClient(final WireMockServer wireMockServer) {
    return newClient(wireMockServer, ToolboxModeratorManagementClient::newClient);
  }

  @Test
  void fetchModeratorList(@WiremockResolver.Wiremock final WireMockServer server) {
    server.stubFor(
        WireMock.get(ToolboxModeratorManagementClient.FETCH_MODERATORS_PATH)
            .withHeader(AuthenticationHeaders.API_KEY_HEADER, equalTo(EXPECTED_API_KEY))
            .willReturn(
                WireMock.aResponse().withStatus(200).withBody(toJson(List.of(MODERATOR_INFO)))));

    final List<ModeratorInfo> results = newClient(server).fetchModeratorList();

    assertThat(results, hasSize(1));
    assertThat(results.get(0), is(MODERATOR_INFO));
  }

  @Nested
  final class IsCurrentUserAdmin {
    @Test
    void positiveCase(@WiremockResolver.Wiremock final WireMockServer server) {
      expectIsAdminAndReturn(server, true);

      assertThat(newClient(server).isCurrentUserAdmin(), is(true));
    }

    void expectIsAdminAndReturn(final WireMockServer server, final boolean value) {
      server.stubFor(
          WireMock.get(ToolboxModeratorManagementClient.IS_ADMIN_PATH)
              .withHeader(AuthenticationHeaders.API_KEY_HEADER, equalTo(EXPECTED_API_KEY))
              .willReturn(WireMock.aResponse().withStatus(200).withBody(String.valueOf(value))));
    }

    @Test
    void negativeCase(@WiremockResolver.Wiremock final WireMockServer server) {
      expectIsAdminAndReturn(server, false);

      assertThat(newClient(server).isCurrentUserAdmin(), is(false));
    }
  }

  @Test
  void removeMod(@WiremockResolver.Wiremock final WireMockServer server) {
    serve200ForToolboxPostWithBody(
        server, ToolboxModeratorManagementClient.REMOVE_MOD_PATH, MODERATOR_NAME);

    newClient(server).removeMod(MODERATOR_NAME);
  }

  @Test
  void addAdmin(@WiremockResolver.Wiremock final WireMockServer server) {
    serve200ForToolboxPostWithBody(
        server, ToolboxModeratorManagementClient.ADD_ADMIN_PATH, MODERATOR_NAME);

    newClient(server).addAdmin(MODERATOR_NAME);
  }

  @Test
  void checkUserExists(@WiremockResolver.Wiremock final WireMockServer server) {
    serve200ForToolboxPostWithBody(
        server, ToolboxModeratorManagementClient.ADD_ADMIN_PATH, MODERATOR_NAME);

    newClient(server).addAdmin(MODERATOR_NAME);
  }

  @Test
  void addModerator(@WiremockResolver.Wiremock final WireMockServer server) {
    serve200ForToolboxPostWithBody(
        server, ToolboxModeratorManagementClient.ADD_MODERATOR_PATH, MODERATOR_NAME);

    newClient(server).addModerator(MODERATOR_NAME);
  }
}
