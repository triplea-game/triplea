package games.strategy.triplea.delegate.battle.simulation;

import java.util.Objects;

/** Result of replaying a recorded episode against a fresh battle environment. */
public record BattleReplayResult(
    boolean matched, int verifiedTransitions, String mismatch, BattleEpisodeLog actualEpisode) {

  public BattleReplayResult {
    if (verifiedTransitions < 0) {
      throw new IllegalArgumentException("verifiedTransitions must not be negative");
    }
    mismatch = mismatch == null ? "" : mismatch;
    Objects.requireNonNull(actualEpisode);
    if (matched && !mismatch.isEmpty()) {
      throw new IllegalArgumentException("matched replay must not contain a mismatch message");
    }
  }
}
