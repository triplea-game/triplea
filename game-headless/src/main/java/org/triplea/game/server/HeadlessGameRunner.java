package org.triplea.game.server;

import org.triplea.config.product.ProductVersionReader;
import org.triplea.injection.Injections;

/** Runs a headless game server. */
public final class HeadlessGameRunner {
  private HeadlessGameRunner() {}

  /**
   * Entry point for running a new headless game server. The headless game server runs until the
   * process is killed or the headless game server is shut down via administrative command.
   */
  public static void main(final String[] args) {
    Injections.init(constructInjections());
    HeadlessGameServer.start(args);
  }

  private static Injections constructInjections() {
    return Injections.builder().engineVersion(new ProductVersionReader().getVersion()).build();
  }
}
