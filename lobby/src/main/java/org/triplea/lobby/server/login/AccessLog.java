package org.triplea.lobby.server.login;

import org.triplea.lobby.server.User;

interface AccessLog {
  void logFailedAuthentication(User user, UserType userType, String errorMessage);

  void logSuccessfulAuthentication(User user, UserType userType);
}
