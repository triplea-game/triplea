package org.triplea.server.http.spark;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.triplea.http.client.lobby.login.LobbyLoginClient;
import org.triplea.http.client.lobby.login.LobbyLoginResponse;
import org.triplea.http.client.lobby.login.RegisteredUserLoginRequest;

import feign.FeignException;

class RegisteredUserLoginControllerTest extends SparkServerSystemTest {
  private static final RegisteredUserLoginRequest REGISTERED_USER_LOGIN_REQUEST =
      RegisteredUserLoginRequest.builder()
          .name("Cannibals whine with death!")
          .password("Scrawny desolations lead to the malaria.")
          .build();

  private static final LobbyLoginResponse STUBBED_RESPONSE = LobbyLoginResponse.newSuccessResponse("token");

  private final LobbyLoginClient client = LobbyLoginClient.newClient(LOCAL_HOST);

  @Test
  void login() {
    when(registeredUserLogin.apply(REGISTERED_USER_LOGIN_REQUEST)).thenReturn(STUBBED_RESPONSE);

    final LobbyLoginResponse response = client.login(REGISTERED_USER_LOGIN_REQUEST);

    assertThat(response, is(STUBBED_RESPONSE));
  }

  @Test
  void errorCase() {
    when(registeredUserLogin.apply(Mockito.any()))
        .thenThrow(new RuntimeException("simulated exception"));

    assertThrows(
        FeignException.class,
        () -> client.login(REGISTERED_USER_LOGIN_REQUEST));
  }
}
