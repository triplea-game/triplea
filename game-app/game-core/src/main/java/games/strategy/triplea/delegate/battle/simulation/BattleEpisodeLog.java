package games.strategy.triplea.delegate.battle.simulation;

import java.util.List;
import java.util.Objects;

/** Serializable deterministic episode record used for replay and regression tests. */
public record BattleEpisodeLog(
    int logSchemaVersion,
    BattleResetRequest resetRequest,
    BattleObservation initialObservation,
    List<BattleTransition> transitions,
    double cumulativeReward,
    boolean terminated,
    boolean truncated) {

  public static final int CURRENT_LOG_SCHEMA_VERSION = 1;

  public BattleEpisodeLog {
    if (logSchemaVersion != CURRENT_LOG_SCHEMA_VERSION) {
      throw new IllegalArgumentException("unsupported logSchemaVersion: " + logSchemaVersion);
    }
    Objects.requireNonNull(resetRequest);
    Objects.requireNonNull(initialObservation);
    transitions = List.copyOf(transitions);
  }

  public BattleObservation finalObservation() {
    return transitions.isEmpty()
        ? initialObservation
        : transitions.getLast().result().observation();
  }
}
