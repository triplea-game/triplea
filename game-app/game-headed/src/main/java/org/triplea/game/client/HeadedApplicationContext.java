package org.triplea.game.client;

import org.triplea.game.ApplicationContext;

/** Implementation of {@link ApplicationContext} for the headed game client. */
public final class HeadedApplicationContext implements ApplicationContext {
  @Override
  public Class<?> getMainClass() {
    return HeadedGameRunner.class;
  }
}
