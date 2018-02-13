package games.strategy.engine.lobby.server.login;

import java.time.Instant;

import games.strategy.engine.lobby.server.User;

interface AccessLog {
  void logFailedAuthentication(Instant instant, User user, UserType userType, String errorMessage);

  void logSuccessfulAuthentication(Instant instant, User user, UserType userType);
}
