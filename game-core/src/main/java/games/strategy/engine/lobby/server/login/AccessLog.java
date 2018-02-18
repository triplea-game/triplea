package games.strategy.engine.lobby.server.login;

import games.strategy.engine.lobby.server.User;

interface AccessLog {
  void logFailedAuthentication(User user, UserType userType, String errorMessage);

  void logSuccessfulAuthentication(User user, UserType userType);
}
