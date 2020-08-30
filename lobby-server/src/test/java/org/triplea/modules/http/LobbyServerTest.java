package org.triplea.modules.http;

import com.github.database.rider.junit5.DBUnitExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.triplea.domain.data.ApiKey;
import org.triplea.test.common.Integration;

/** Core configuration for a test that will start a dropwizard server and initialize database. */
@Integration
@ExtendWith(value = {LobbyServerExtension.class, DBUnitExtension.class})
@SuppressWarnings("PrivateConstructorForUtilityClass")
public abstract class LobbyServerTest {
  public static final String LOBBY_USER_DATASET =
      "integration/user_role.yml,"
          + "integration/lobby_user.yml,"
          + "integration/lobby_api_key.yml";
  protected static final ApiKey MODERATOR_API_KEY = AllowedUserRole.MODERATOR.getAllowedKey();
  protected static final ApiKey CHATTER_API_KEY = AllowedUserRole.PLAYER.getAllowedKey();
}
