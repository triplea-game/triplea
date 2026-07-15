package games.strategy.triplea.delegate.reinforcement;

import games.strategy.engine.data.GamePlayer;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Serializable progress and queue state for fixed reinforcements. */
public final class FixedReinforcementTracker implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  private final Map<String, Integer> lastProcessedRoundByPlayer = new HashMap<>();
  private final Map<String, List<FixedReinforcementOrder>> pendingByPlayer = new HashMap<>();

  public int getLastProcessedRound(final GamePlayer player) {
    return lastProcessedRoundByPlayer.getOrDefault(player.getName(), 0);
  }

  public List<FixedReinforcementOrder> getPending(final GamePlayer player) {
    return List.copyOf(pendingByPlayer.getOrDefault(player.getName(), List.of()));
  }

  boolean shouldProcess(final GamePlayer player, final int currentRound) {
    return currentRound > getLastProcessedRound(player);
  }

  List<FixedReinforcementOrder> getOrdersForRound(
      final GamePlayer player,
      final int currentRound,
      final List<FixedReinforcementRule> schedule) {
    if (!shouldProcess(player, currentRound)) {
      return List.of();
    }
    final int lastProcessedRound = getLastProcessedRound(player);
    final List<FixedReinforcementOrder> orders = new ArrayList<>(getPending(player));
    schedule.stream()
        .filter(rule -> rule.round() > lastProcessedRound && rule.round() <= currentRound)
        .map(FixedReinforcementRule::toOrder)
        .forEach(orders::add);
    return List.copyOf(orders);
  }

  void completeRound(
      final GamePlayer player,
      final int currentRound,
      final List<FixedReinforcementOrder> remainingOrders) {
    lastProcessedRoundByPlayer.put(player.getName(), currentRound);
    if (remainingOrders.isEmpty()) {
      pendingByPlayer.remove(player.getName());
    } else {
      pendingByPlayer.put(player.getName(), List.copyOf(remainingOrders));
    }
  }
}
