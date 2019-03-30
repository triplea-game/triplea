package org.triplea.server.http.spark.controller;

import static spark.Spark.post;

import java.util.function.Function;

import org.triplea.http.client.lobby.login.LobbyLoginResponse;

import com.google.gson.Gson;

import lombok.AllArgsConstructor;
import spark.Request;

/**
 * Controller that routes requests to authenticate 'anonymous' users. An anonymous user login consists
 * only of the desired username. The return value of the controller is a JSON with a 'login token'. If
 * the token is missing then login for that username is denied. Otherwise the token can be used
 * to establish a socket connection with the lobby.
 */
@AllArgsConstructor
public class AnonymousUserLoginController implements Runnable {

  private static final String ANONYMOUS_LOGIN_PATH = "/anonymous-login";

  private final Function<String, LobbyLoginResponse> anonymousUserLogin;

  @Override
  public void run() {
    post(ANONYMOUS_LOGIN_PATH, (req, res) -> doLogin(req));
  }

  private String doLogin(final Request request) {
    final String loginName = new Gson().fromJson(request.body(), String.class);
    final LobbyLoginResponse result = anonymousUserLogin.apply(loginName);
    return new Gson().toJson(result);
  }
}
