package games.strategy.triplea.delegate.reinforcement;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameState;
import games.strategy.triplea.attachments.FixedReinforcementAttachment;
import java.util.List;

/** Builds a stable reinforcement observation for future strategic environments. */
public final class FixedReinforcementObservationFactory {
  private FixedReinforcementObservationFactory() {}

  public static FixedReinforcementObservation create(
      final GameState data, final GamePlayer player, final FixedReinforcementTracker tracker) {
    final int currentRound = data.getSequence().getRound();
    final List<FixedReinforcementObservation.Entry> pending =
        tracker.getPending(player).stream().map(FixedReinforcementObservation.Entry::from).toList();
    final List<FixedReinforcementObservation.Entry> scheduled =
        FixedReinforcementAttachment.get(player)
            .map(FixedReinforcementAttachment::getReinforcements)
            .orElse(List.of())
            .stream()
            .filter(rule -> rule.round() > tracker.getLastProcessedRound(player))
            .map(FixedReinforcementObservation.Entry::from)
            .toList();
    return new FixedReinforcementObservation(
        FixedReinforcementObservation.SCHEMA_VERSION,
        player.getName(),
        currentRound,
        tracker.getLastProcessedRound(player),
        pending,
        scheduled);
  }
}
