package org.triplea.game.server;

import org.triplea.game.ApplicationContext;

/** Implementation of {@link ApplicationContext} for the headless game server. */
public final class HeadlessApplicationContext implements ApplicationContext {
  @Override
  public Class<?> getMainClass() {
    return HeadlessGameRunner.class;
  }
}
