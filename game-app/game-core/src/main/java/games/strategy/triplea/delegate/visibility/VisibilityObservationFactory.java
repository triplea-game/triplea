package games.strategy.triplea.delegate.visibility;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameState;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/** Converts mutable game state into a deterministic player-filtered visibility observation. */
public final class VisibilityObservationFactory {
  private VisibilityObservationFactory() {}

  public static VisibilityObservation create(final GameState data, final GamePlayer viewer) {
    final Set<Territory> visible = VisibilityService.getVisibleTerritories(viewer, data);
    final List<Territory> territories = new ArrayList<>(data.getMap().getTerritories());
    territories.sort(Comparator.comparing(Territory::getName));

    final List<VisibilityObservation.TerritoryState> states = new ArrayList<>();
    for (final Territory territory : territories) {
      final boolean territoryVisible = visible.contains(territory);
      states.add(
          new VisibilityObservation.TerritoryState(
              territory.getName(),
              territory.isWater(),
              territoryVisible,
              territoryVisible ? territory.getOwner().getName() : null,
              data.getMap().getNeighbors(territory).stream()
                  .map(Territory::getName)
                  .sorted()
                  .toList(),
              territoryVisible ? groupUnits(territory) : List.of()));
    }

    return new VisibilityObservation(
        VisibilityObservation.CURRENT_SCHEMA_VERSION,
        data.getSequence().getRound(),
        viewer.getName(),
        VisibilityService.getVisionRadius(data),
        states);
  }

  private static List<VisibilityObservation.UnitGroup> groupUnits(final Territory territory) {
    final Map<UnitKey, Integer> counts =
        new TreeMap<>(Comparator.comparing(UnitKey::owner).thenComparing(UnitKey::unitType));
    for (final Unit unit : territory.getUnitCollection().getUnits()) {
      counts.merge(
          new UnitKey(unit.getOwner().getName(), unit.getType().getName()), 1, Integer::sum);
    }
    return counts.entrySet().stream()
        .map(
            entry ->
                new VisibilityObservation.UnitGroup(
                    entry.getKey().owner(), entry.getKey().unitType(), entry.getValue()))
        .toList();
  }

  private record UnitKey(String owner, String unitType) {}
}
