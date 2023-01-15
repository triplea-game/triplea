package org.triplea.spitfire.server.access.authorization;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import feign.FeignException;
import java.net.URI;
import java.util.Map;
import lombok.AllArgsConstructor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.triplea.http.client.LobbyHttpClientConfig;
import org.triplea.http.client.lobby.login.LobbyLoginClient;
import org.triplea.http.client.lobby.login.LobbyLoginResponse;
import org.triplea.spitfire.server.ControllerIntegrationTest;

/**
 * In this test we'll verify that user-banned filter is configured. We'll do ban by system-id as
 * that is easier to control and does not need us to ban localhost. We'll verify the ban against the
 * login endpoint as that will be a common first endpoint for banned users to attempt to access
 * after being booted and banned.
 */
@AllArgsConstructor
class BannedPlayerFilterIntegrationTest extends ControllerIntegrationTest {
  private static final String BANNED_SYSTEM_ID = "system-id";

  private final URI localhost;

  @Test
  void banned() {
    // we expect an exception because user is banned. If user were not banned
    // then the login attempt would return result.
    final FeignException exception =
        Assertions.assertThrows(
            FeignException.class,
            () ->
                LobbyLoginClient.newClient(localhost, headersWithSystemId(BANNED_SYSTEM_ID))
                    .login("user", null));

    assertThat(exception.status(), is(401));
  }

  @Test
  void notBanned() {
    // a not-banned user should be able to login with anonymouse user account
    final LobbyLoginResponse lobbyLoginResponse =
        LobbyLoginClient.newClient(localhost, headersWithSystemId("any other system id"))
            .login("user", null);
    assertThat(lobbyLoginResponse.isSuccess(), is(true));
  }

  @SuppressWarnings("SameParameterValue")
  private static Map<String, String> headersWithSystemId(final String systemId) {
    return Map.of(LobbyHttpClientConfig.SYSTEM_ID_HEADER, systemId);
  }
}
