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

  public static final int CURRENT_SCHEMA_VERSION = 1;

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
      List<String> neighbors,
      List<UnitGroup> units) {
    public TerritoryState {
      Objects.requireNonNull(territory);
      neighbors = List.copyOf(neighbors);
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
      String minimumMovementLeft) {
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
