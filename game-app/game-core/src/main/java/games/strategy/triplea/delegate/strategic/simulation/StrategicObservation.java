package games.strategy.triplea.delegate.strategic.simulation;

import games.strategy.triplea.delegate.battle.simulation.BattleObservation;
import games.strategy.triplea.delegate.reinforcement.FixedReinforcementObservation;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;

/** Visibility-filtered graph observation for one complete player turn. */
public record StrategicObservation(
    int schemaVersion,
    long seed,
    int round,
    String player,
    String sequenceStep,
    StrategicPhase phase,
    StrategicDecisionDomain decisionDomain,
    List<TerritoryState> territories,
    FixedReinforcementObservation reinforcements,
    List<PendingBattle> pendingBattles,
    @Nullable BattleObservation battle,
    boolean over) {

  public static final int CURRENT_SCHEMA_VERSION = 2;

  public StrategicObservation {
    Objects.requireNonNull(player);
    Objects.requireNonNull(sequenceStep);
    Objects.requireNonNull(phase);
    Objects.requireNonNull(decisionDomain);
    territories = List.copyOf(territories);
    Objects.requireNonNull(reinforcements);
    pendingBattles = List.copyOf(pendingBattles);
  }

  public record TerritoryState(
      String territory,
      boolean water,
      boolean visible,
      @Nullable String owner,
      @Nullable Boolean supplied,
      boolean supplySource,
      @Nullable String airControlPlayer,
      @Nullable String airControlStatus,
      @Nullable Boolean airControlPersistent,
      List<String> neighbors,
      List<String> roadConnections,
      List<UnitGroup> units) {
    public TerritoryState {
      Objects.requireNonNull(territory);
      neighbors = List.copyOf(neighbors);
      roadConnections = List.copyOf(roadConnections);
      units = List.copyOf(units);
    }
  }

  public record UnitGroup(
      String owner,
      String unitType,
      int count,
      boolean land,
      boolean air,
      boolean sea,
      String minimumMovementLeft,
      @Nullable Boolean supplied,
      @Nullable Integer outOfSupplyTurns,
      @Nullable Integer turnsUntilRemoval) {
    public UnitGroup {
      Objects.requireNonNull(owner);
      Objects.requireNonNull(unitType);
      Objects.requireNonNull(minimumMovementLeft);
      if (count < 1) {
        throw new IllegalArgumentException("unit-group count must be positive");
      }
    }
  }

  public record PendingBattle(String battleId, String territory, String battleType) {
    public PendingBattle {
      Objects.requireNonNull(battleId);
      Objects.requireNonNull(territory);
      Objects.requireNonNull(battleType);
    }
  }
}
