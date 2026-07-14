package games.strategy.triplea.delegate.supply;

import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.history.IDelegateHistoryWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Updates supply counters and removes units that remain isolated for too long. */
public final class SupplyService {
  private SupplyService() {}

  public static void apply(
      final IDelegateBridge bridge, final GamePlayer player, final SupplyTracker tracker) {
    final GameData data = bridge.getData();
    if (!SupplyNetworkResolver.isEnabled(data)) {
      return;
    }
    final int round = data.getSequence().getRound();
    if (!tracker.shouldProcess(player, round)) {
      return;
    }

    tracker.retainExistingUnits(data.getUnits().getUnits());
    final int removalTurns = SupplyNetworkResolver.getRemovalTurns(data);
    final IDelegateHistoryWriter history = bridge.getHistoryWriter();
    history.startEvent("Supply status for " + player.getName() + " in round " + round);

    final Map<Territory, List<Unit>> removals = new LinkedHashMap<>();
    final List<Territory> territories = new ArrayList<>(data.getMap().getTerritories());
    territories.sort(Comparator.comparing(Territory::getName));
    for (final Territory territory : territories) {
      if (territory.isWater()) {
        continue;
      }
      final boolean supplied = SupplyNetworkResolver.isSupplied(territory, player, data);
      final List<Unit> units =
          territory.getUnitCollection().getUnits().stream()
              .filter(unit -> unit.isOwnedBy(player))
              .filter(SupplyNetworkResolver::requiresSupply)
              .sorted(Comparator.comparing(unit -> unit.getId().toString()))
              .toList();
      for (final Unit unit : units) {
        final int previousTurns = tracker.getOutOfSupplyTurns(unit);
        if (supplied) {
          tracker.clear(unit);
          if (previousTurns > 0) {
            history.addChildToEvent(
                unit.getType().getName() + " in " + territory.getName() + " is supplied again",
                List.of(unit));
          }
          continue;
        }

        final int turns = tracker.increment(unit);
        if (turns >= removalTurns) {
          removals.computeIfAbsent(territory, ignored -> new ArrayList<>()).add(unit);
          tracker.clear(unit);
          history.addChildToEvent(
              "Removed "
                  + unit.getType().getName()
                  + " from "
                  + territory.getName()
                  + " after "
                  + turns
                  + " owner turns without supply",
              List.of(unit));
        } else {
          history.addChildToEvent(
              unit.getType().getName()
                  + " in "
                  + territory.getName()
                  + " has been out of supply for "
                  + turns
                  + " of "
                  + removalTurns
                  + " owner turns",
              List.of(unit));
        }
      }
    }

    final CompositeChange removalChange = new CompositeChange();
    removals.forEach(
        (territory, units) -> removalChange.add(ChangeFactory.removeUnits(territory, units)));
    if (!removalChange.isEmpty()) {
      bridge.addChange(removalChange);
    }
    tracker.completeRound(player, round);
  }
}
