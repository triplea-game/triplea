package games.strategy.triplea.delegate.supply;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Unit;
import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

/** Tracks consecutive owner turns spent without supply. */
public final class SupplyTracker implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  private final Map<String, Integer> lastProcessedRoundByPlayer = new HashMap<>();
  private final Map<UUID, Status> statusByUnit = new HashMap<>();

  public boolean shouldProcess(final GamePlayer player, final int round) {
    return getLastProcessedRound(player) < round;
  }

  public int getLastProcessedRound(final GamePlayer player) {
    return lastProcessedRoundByPlayer.getOrDefault(player.getName(), 0);
  }

  public int getOutOfSupplyTurns(final Unit unit) {
    final Status status = statusByUnit.get(unit.getId());
    return status != null && status.ownerName().equals(unit.getOwner().getName())
        ? status.turns()
        : 0;
  }

  public int increment(final Unit unit) {
    final String ownerName = unit.getOwner().getName();
    final Status previous = statusByUnit.get(unit.getId());
    final int turns =
        previous != null && previous.ownerName().equals(ownerName) ? previous.turns() + 1 : 1;
    statusByUnit.put(unit.getId(), new Status(ownerName, turns));
    return turns;
  }

  public void clear(final Unit unit) {
    statusByUnit.remove(unit.getId());
  }

  public void retainExistingUnits(final Collection<Unit> units) {
    final Set<UUID> existing = units.stream().map(Unit::getId).collect(Collectors.toSet());
    statusByUnit.keySet().retainAll(existing);
  }

  public void completeRound(final GamePlayer player, final int round) {
    lastProcessedRoundByPlayer.put(player.getName(), round);
  }

  public Map<String, Integer> snapshot() {
    final Map<String, Integer> snapshot = new TreeMap<>();
    statusByUnit.forEach((id, status) -> snapshot.put(id.toString(), status.turns()));
    return Map.copyOf(snapshot);
  }

  private record Status(String ownerName, int turns) implements Serializable {
    @Serial private static final long serialVersionUID = 1L;
  }
}
