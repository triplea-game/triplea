package games.strategy.triplea.delegate.supply;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameState;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.attachments.SupplyTerritoryAttachment;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Converts mutable supply state into a deterministic observation DTO. */
public final class SupplyObservationFactory {
  private SupplyObservationFactory() {}

  public static SupplyObservation create(
      final GameState data, final GamePlayer player, final SupplyTracker tracker) {
    final int removalTurns = SupplyNetworkResolver.getRemovalTurns(data);
    final List<Territory> territories = new ArrayList<>(data.getMap().getTerritories());
    territories.sort(Comparator.comparing(Territory::getName));

    final List<SupplyObservation.TerritoryState> territoryStates = new ArrayList<>();
    final List<SupplyObservation.UnitState> unitStates = new ArrayList<>();
    for (final Territory territory : territories) {
      if (territory.isWater()) {
        continue;
      }
      final boolean friendly = isFriendly(territory, player, data);
      final boolean supplied = SupplyNetworkResolver.isSupplied(territory, player, data);
      final boolean source =
          SupplyTerritoryAttachment.get(territory)
              .map(SupplyTerritoryAttachment::getSupplySource)
              .orElse(false);
      final List<String> roadConnections =
          SupplyNetworkResolver.getRoadNeighbors(territory, data).stream()
              .map(Territory::getName)
              .sorted()
              .toList();
      territoryStates.add(
          new SupplyObservation.TerritoryState(
              territory.getName(), friendly, supplied, source, roadConnections));

      for (final Unit unit :
          territory.getUnitCollection().getUnits().stream()
              .filter(candidate -> candidate.isOwnedBy(player))
              .filter(SupplyNetworkResolver::requiresSupply)
              .sorted(Comparator.comparing(candidate -> candidate.getId().toString()))
              .toList()) {
        final int turns = tracker.getOutOfSupplyTurns(unit);
        unitStates.add(
            new SupplyObservation.UnitState(
                unit.getId().toString(),
                territory.getName(),
                unit.getType().getName(),
                supplied,
                turns,
                supplied ? 0 : Math.max(0, removalTurns - turns)));
      }
    }

    return new SupplyObservation(
        SupplyObservation.CURRENT_SCHEMA_VERSION,
        data.getSequence().getRound(),
        tracker.getLastProcessedRound(player),
        player.getName(),
        removalTurns,
        territoryStates,
        unitStates);
  }

  private static boolean isFriendly(
      final Territory territory, final GamePlayer player, final GameState data) {
    final GamePlayer owner = territory.getOwner();
    return player.equals(owner)
        || (data.getRelationshipTracker().getRelationship(player, owner) != null
            && data.getRelationshipTracker().isAllied(player, owner));
  }
}
