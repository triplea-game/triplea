package org.triplea.server.http.spark;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.triplea.http.client.lobby.login.LobbyLoginClient;
import org.triplea.http.client.lobby.login.LobbyLoginResponse;

import feign.FeignException;

class AnonymousUserLoginControllerTest extends SparkServerSystemTest {
  private static final String LOGIN_NAME = "Ah, fine whale. Vandalize the lighthouse!";
  private static final LobbyLoginResponse STUBBED_RESPONSE = LobbyLoginResponse.newSuccessResponse("token");

  private final LobbyLoginClient client = LobbyLoginClient.newClient(LOCAL_HOST);


  @Test
  void login() {
    when(anonymousUserLogin.apply(LOGIN_NAME)).thenReturn(STUBBED_RESPONSE);

    final LobbyLoginResponse response = client.anonymousLogin(LOGIN_NAME);

    assertThat(response, is(STUBBED_RESPONSE));
  }


  @Test
  void errorCase() {
    when(anonymousUserLogin.apply(Mockito.any()))
        .thenThrow(new RuntimeException("simulated exception"));

    assertThrows(
        FeignException.class,
        () -> client.anonymousLogin(LOGIN_NAME));
  }
}
