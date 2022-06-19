package org.triplea.http.client.lobby.login;

import feign.RequestLine;

/** Http client to authenticate a user with the lobby or create an account. */
interface LobbyLoginFeignClient {
  @RequestLine("POST " + LobbyLoginClient.LOGIN_PATH)
  LobbyLoginResponse login(LoginRequest loginRequest);

  @RequestLine("POST " + LobbyLoginClient.CREATE_ACCOUNT)
  CreateAccountResponse createAccount(CreateAccountRequest build);
}
