package org.triplea.ai.benchmark.metrics;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import java.util.Collection;

/**
 * A game-state number summed over a group of players. The runner takes it for the seat and for the
 * enemy and records the gap between them, so a metric never computes a gap itself.
 */
public interface Metric {
  /** Stable key used in kata descriptors and the raw data. */
  String id();

  double value(GameData data, Collection<GamePlayer> players);
}
