package games.strategy.triplea.delegate.battle.simulation;

import java.util.List;
import java.util.Objects;

/** Stable, UI-independent snapshot of one battle decision context. */
public record BattleObservation(
    int schemaVersion,
    String battleId,
    String territory,
    int round,
    int maxRounds,
    boolean over,
    boolean amphibious,
    boolean headless,
    String offensePlayer,
    String defensePlayer,
    List<UnitGroupObservation> offense,
    List<UnitGroupObservation> defense,
    List<String> attackerRetreatTerritories) {

  public static final int CURRENT_SCHEMA_VERSION = 1;

  public BattleObservation {
    Objects.requireNonNull(battleId);
    Objects.requireNonNull(territory);
    Objects.requireNonNull(offensePlayer);
    Objects.requireNonNull(defensePlayer);
    offense = List.copyOf(offense);
    defense = List.copyOf(defense);
    attackerRetreatTerritories = List.copyOf(attackerRetreatTerritories);
  }
}
