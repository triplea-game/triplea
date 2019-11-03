package org.triplea.server;

import lombok.experimental.UtilityClass;
import org.triplea.domain.data.ApiKey;
import org.triplea.lobby.server.db.data.UserRole;
import org.triplea.server.access.AuthenticatedUser;

@UtilityClass
public class TestData {
  public static final ApiKey API_KEY = ApiKey.of("test");

  public static final AuthenticatedUser AUTHENTICATED_USER =
      AuthenticatedUser.builder().userId(100).userRole(UserRole.PLAYER).apiKey(API_KEY).build();
}
