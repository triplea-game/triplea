package org.triplea.http.client.lobby.login;

import com.google.common.annotations.VisibleForTesting;
import java.net.URI;
import java.util.Map;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.triplea.http.client.HttpClient;
import org.triplea.http.client.lobby.AuthenticationHeaders;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class LobbyLoginClient {
  public static final String LOGIN_PATH = "/user-login/authenticate";
  public static final String CREATE_ACCOUNT = "/user-login/create-account";

  private final LobbyLoginFeignClient lobbyLoginFeignClient;

  public static LobbyLoginClient newClient(final URI uri) {
    return newClient(uri, AuthenticationHeaders.systemIdHeaders());
  }

  @VisibleForTesting
  public static LobbyLoginClient newClient(final URI uri, final Map<String, String> headers) {
    return new LobbyLoginClient(HttpClient.newClient(LobbyLoginFeignClient.class, uri, headers));
  }

  public LobbyLoginResponse login(final String userName, final String password) {
    return lobbyLoginFeignClient.login(
        LoginRequest.builder().name(userName).password(password).build());
  }

  public CreateAccountResponse createAccount(
      final String username, final String email, final String password) {
    return lobbyLoginFeignClient.createAccount(
        CreateAccountRequest.builder().username(username).email(email).password(password).build());
  }
}
