package org.triplea.game.client;

import games.strategy.engine.framework.GameRunner;

/**
 * Runs a headed game client.
 */
public final class HeadedGameRunner {
  private HeadedGameRunner() {}

  /**
   * Entry point for running a new headed game client.
   */
  public static void main(final String[] args) {
    GameRunner.start(args);
  }
}
