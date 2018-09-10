package org.triplea.game.headed.runner;

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
    GameRunner.start(GameRunner.Context.builder()
        .args(args)
        .mainClass(HeadedGameRunner.class)
        .build());
  }
}
