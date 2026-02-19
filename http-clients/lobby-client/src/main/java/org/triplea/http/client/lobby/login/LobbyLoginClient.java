package org.triplea.http.client.lobby.login;

import feign.RequestLine;
import java.net.URI;
import java.util.Map;
import org.triplea.http.client.HttpClient;
import org.triplea.http.client.lobby.AuthenticationHeaders;

public interface LobbyLoginClient {
  String LOGIN_PATH = "/lobby/user-login/authenticate";
  String CREATE_ACCOUNT = "/lobby/user-login/create-account";

  static LobbyLoginClient newClient(final URI uri) {
    return newClient(uri, AuthenticationHeaders.systemIdHeaders());
  }

  static LobbyLoginClient newClient(final URI uri, final Map<String, String> headers) {
    return HttpClient.newClient(LobbyLoginClient.class, uri, headers);
  }

  @RequestLine("POST " + LobbyLoginClient.LOGIN_PATH)
  LobbyLoginResponse login(LoginRequest loginRequest);

  default LobbyLoginResponse login(final String userName, final String password) {
    return login(LoginRequest.builder().name(userName).password(password).build());
  }

  @RequestLine("POST " + LobbyLoginClient.CREATE_ACCOUNT)
  CreateAccountResponse createAccount(CreateAccountRequest build);

  default CreateAccountResponse createAccount(String username, String email, String password) {
    return createAccount(
        CreateAccountRequest.builder() //
            .username(username)
            .email(email)
            .password(password)
            .build());
  }
}
