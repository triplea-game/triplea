package games.strategy.triplea.delegate.supply;

import java.util.List;
import java.util.Objects;

/** Stable strategic-agent snapshot of one player's supply network. */
public record SupplyObservation(
    int schemaVersion,
    int currentRound,
    int lastProcessedRound,
    String player,
    int removalTurns,
    List<TerritoryState> territories,
    List<UnitState> units) {

  public static final int CURRENT_SCHEMA_VERSION = 2;

  public SupplyObservation {
    Objects.requireNonNull(player);
    territories = List.copyOf(territories);
    units = List.copyOf(units);
  }

  public record TerritoryState(
      String territory,
      boolean friendly,
      boolean supplied,
      boolean supplySource,
      List<String> roadConnections) {
    public TerritoryState {
      Objects.requireNonNull(territory);
      roadConnections = List.copyOf(roadConnections);
    }
  }

  public record UnitState(
      String unitId,
      String territory,
      String unitType,
      boolean supplied,
      int outOfSupplyTurns,
      int turnsUntilRemoval) {
    public UnitState {
      Objects.requireNonNull(unitId);
      Objects.requireNonNull(territory);
      Objects.requireNonNull(unitType);
    }
  }
}
