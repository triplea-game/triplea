package org.triplea.http.client.lobby.login;

import java.net.URI;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.triplea.http.client.AuthenticationHeaders;
import org.triplea.http.client.HttpClient;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class LobbyLoginClient {
  public static final String LOGIN_PATH = "/user-login/authenticate";
  public static final String CREATE_ACCOUNT = "/user-login/create-account";

  private final LobbyLoginFeignClient lobbyLoginFeignClient;

  public static LobbyLoginClient newClient(final URI uri) {
    return new LobbyLoginClient(new HttpClient<>(LobbyLoginFeignClient.class, uri).get());
  }

  public LobbyLoginResponse login(final String userName, final String password) {
    return lobbyLoginFeignClient.login(
        AuthenticationHeaders.systemIdHeaders(),
        LoginRequest.builder().name(userName).password(password).build());
  }

  public CreateAccountResponse createAccount(
      final String username, final String email, final String password) {
    return lobbyLoginFeignClient.createAccount(
        AuthenticationHeaders.systemIdHeaders(),
        CreateAccountRequest.builder().username(username).email(email).password(password).build());
  }
}
