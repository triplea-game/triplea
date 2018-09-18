package org.triplea.game.server;

import games.strategy.engine.framework.headlessGameServer.HeadlessGameServer;

/**
 * Runs a headless game server.
 */
public final class HeadlessGameRunner {
  private HeadlessGameRunner() {}

  /**
   * Entry point for running a new headless game server. The headless game server runs until the process is killed or
   * the headless game server is shut down via administrative command.
   */
  public static void main(final String[] args) {
    HeadlessGameServer.start(args);
  }
}
