package org.triplea.http.client.lobby.login;

import com.google.common.annotations.VisibleForTesting;
import java.net.URI;
import java.util.Map;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.triplea.http.client.AuthenticationHeaders;
import org.triplea.http.client.HttpClient;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class LobbyLoginClient {
  public static final String LOGIN_PATH = "/user-login/authenticate";
  public static final String CREATE_ACCOUNT = "/user-login/create-account";

  private final LobbyLoginFeignClient lobbyLoginFeignClient;
  private final Map<String, Object> headers;

  public static LobbyLoginClient newClient(final URI uri) {
    return newClient(uri, AuthenticationHeaders.systemIdHeaders());
  }

  @VisibleForTesting
  public static LobbyLoginClient newClient(final URI uri, final Map<String, Object> headers) {
    return new LobbyLoginClient(new HttpClient<>(LobbyLoginFeignClient.class, uri).get(), headers);
  }

  public LobbyLoginResponse login(final String userName, final String password) {
    return lobbyLoginFeignClient.login(
        headers, LoginRequest.builder().name(userName).password(password).build());
  }

  public CreateAccountResponse createAccount(
      final String username, final String email, final String password) {
    return lobbyLoginFeignClient.createAccount(
        AuthenticationHeaders.systemIdHeaders(),
        CreateAccountRequest.builder().username(username).email(email).password(password).build());
  }
}
