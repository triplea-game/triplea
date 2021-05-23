package org.triplea.spitfire.server;

import io.dropwizard.testing.DropwizardTestSupport;
import org.triplea.domain.data.ApiKey;

public class SpitfireServerTestExtension extends DropwizardServerExtension<SpitfireServerConfig> {

  public static final String LOBBY_USER_DATASET =
      "integration/user_role.yml,"
          + "integration/lobby_user.yml,"
          + "integration/lobby_api_key.yml";
  public static final ApiKey MODERATOR_API_KEY = AllowedUserRole.MODERATOR.getAllowedKey();
  public static final ApiKey CHATTER_API_KEY = AllowedUserRole.PLAYER.getAllowedKey();

  private static final DropwizardTestSupport<SpitfireServerConfig> testSupport =
      new DropwizardTestSupport<>(SpitfireServerApplication.class, "configuration.yml");

  @Override
  public DropwizardTestSupport<SpitfireServerConfig> getSupport() {
    return testSupport;
  }
}
