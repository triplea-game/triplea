package org.triplea.http.client.lobby.login;

import feign.Headers;
import feign.RequestLine;
import java.net.URI;
import org.triplea.http.client.HttpClient;
import org.triplea.http.client.HttpConstants;

/**
 * Http client to authenticate a user with the http(s)-lobby. Both registered and anonymous users
 * use this to gain a single-use token that can be used to establish a non-https socket connection.
 */
@SuppressWarnings("InterfaceNeverImplemented")
@Headers({HttpConstants.CONTENT_TYPE_JSON, HttpConstants.ACCEPT_JSON})
public interface LobbyLoginClient {

  String LOGIN_PATH = "/login/registered-user";
  String ANONYMOUS_LOGIN_PATH = "/login/anonymous-user";
  String HOST_GAME_PATH = "/login/host-game";

  static LobbyLoginClient newClient(final URI uri) {
    return new HttpClient<>(LobbyLoginClient.class, uri).get();
  }

  /**
   * Http client method to do username and password verification. Example usage:
   *
   * <pre>
   * LobbyLoginClient client = LobbyLoginClient.newClient(uri);
   * try {
   *   String token = client.login(name, password);
   *   if (!token.isPresent()) {
   *     // login failed, bad credentials
   *   }
   * } catch (FeignException e) {
   *   // communication or server error
   * }
   * </pre>
   */
  @RequestLine("POST " + LOGIN_PATH)
  LobbyLoginResponse login(RegisteredUserLoginRequest loginRequest);

  /**
   * Http client method to for anonymous login, should only check that a given username is not
   * reserved nor violates any rules. Example usage:
   *
   * <pre>
   * LobbyLoginClient client = LobbyLoginClient.newClient(uri);
   * try {
   *   String token = client.anonymousLogin(name);
   *   if (!token.isPresent()) {
   *     // login failed, bad credentials
   *   }
   * } catch (FeignException e) {
   *   // communication or server error
   * }
   * </pre>
   */
  @RequestLine("POST " + ANONYMOUS_LOGIN_PATH)
  LobbyLoginResponse anonymousLogin(String name);

  @RequestLine("POST " + HOST_GAME_PATH)
  LobbyLoginResponse hostGame();
}
