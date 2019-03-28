package org.triplea.server.http.spark.controller;

import static spark.Spark.post;

import java.util.function.Function;

import org.triplea.http.client.lobby.login.LobbyLoginResponse;
import org.triplea.http.client.lobby.login.RegisteredUserLoginRequest;

import com.google.gson.Gson;

import lombok.AllArgsConstructor;
import spark.Request;

/**
 * Controller that routes requests to authenticate 'registered' users. Registered users have previously added
 * a password to their account, a registered login requests consists of username and password.
 * Return value is a JSON with a 'login token'. If the token is missing then login for that username+password
 * combination is denied. Otherwise, the token can be used to establish a socket connection with the lobby.
 */
@AllArgsConstructor
public class RegisteredUserLoginController implements Runnable {

  private static final String REGISTERED_LOGIN_PATH = "/login";

  private final Function<RegisteredUserLoginRequest, LobbyLoginResponse> registeredUserLogin;

  @Override
  public void run() {
    post(REGISTERED_LOGIN_PATH, (req, res) -> doLogin(req));
  }

  private String doLogin(final Request req) {
    final RegisteredUserLoginRequest loginRequest = readLoginRequest(req);
    final LobbyLoginResponse result = registeredUserLogin.apply(loginRequest);
    return new Gson().toJson(result);
  }

  private static RegisteredUserLoginRequest readLoginRequest(final Request request) {
    return new Gson().fromJson(request.body(), RegisteredUserLoginRequest.class);
  }
}
