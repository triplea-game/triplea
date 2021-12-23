package org.triplea.http.client.lobby.login;

import feign.HeaderMap;
import feign.Headers;
import feign.RequestLine;
import java.util.Map;
import org.triplea.http.client.HttpConstants;

/** Http client to authenticate a user with the lobby or create an account. */
@Headers({HttpConstants.CONTENT_TYPE_JSON, HttpConstants.ACCEPT_JSON})
interface LobbyLoginFeignClient {
  @RequestLine("POST " + LobbyLoginClient.LOGIN_PATH)
  LobbyLoginResponse login(@HeaderMap Map<String, Object> headerMap, LoginRequest loginRequest);

  @RequestLine("POST " + LobbyLoginClient.CREATE_ACCOUNT)
  CreateAccountResponse createAccount(
      @HeaderMap Map<String, Object> headerMap, CreateAccountRequest build);
}
