package org.triplea.game.client;

import org.triplea.game.client.ui.javafx.TripleA;

import games.strategy.engine.framework.GameRunner;
import javafx.application.Application;

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
        .startJavaFxClient(HeadedGameRunner::startJavaFxClient)
        .build());
  }

  private static void startJavaFxClient(final String[] args) {
    Application.launch(TripleA.class, args);
  }
}
