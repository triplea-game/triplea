package games.strategy.triplea.delegate.battle.simulation;

import java.util.List;
import java.util.Objects;

/** Stable, UI-independent snapshot of one battle decision context. */
public record BattleObservation(
    int schemaVersion,
    long seed,
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
    List<String> attackerRetreatTerritories,
    String airControlPlayer,
    int offenseGroundAttackBonus,
    BattleDecisionObservation decision) {

  public static final int CURRENT_SCHEMA_VERSION = 4;

  public BattleObservation(
      final int schemaVersion,
      final String battleId,
      final String territory,
      final int round,
      final int maxRounds,
      final boolean over,
      final boolean amphibious,
      final boolean headless,
      final String offensePlayer,
      final String defensePlayer,
      final List<UnitGroupObservation> offense,
      final List<UnitGroupObservation> defense,
      final List<String> attackerRetreatTerritories) {
    this(
        schemaVersion,
        0,
        battleId,
        territory,
        round,
        maxRounds,
        over,
        amphibious,
        headless,
        offensePlayer,
        defensePlayer,
        offense,
        defense,
        attackerRetreatTerritories,
        "",
        0,
        BattleDecisionObservation.none());
  }

  public BattleObservation(
      final int schemaVersion,
      final long seed,
      final String battleId,
      final String territory,
      final int round,
      final int maxRounds,
      final boolean over,
      final boolean amphibious,
      final boolean headless,
      final String offensePlayer,
      final String defensePlayer,
      final List<UnitGroupObservation> offense,
      final List<UnitGroupObservation> defense,
      final List<String> attackerRetreatTerritories) {
    this(
        schemaVersion,
        seed,
        battleId,
        territory,
        round,
        maxRounds,
        over,
        amphibious,
        headless,
        offensePlayer,
        defensePlayer,
        offense,
        defense,
        attackerRetreatTerritories,
        "",
        0,
        BattleDecisionObservation.none());
  }

  public BattleObservation(
      final int schemaVersion,
      final long seed,
      final String battleId,
      final String territory,
      final int round,
      final int maxRounds,
      final boolean over,
      final boolean amphibious,
      final boolean headless,
      final String offensePlayer,
      final String defensePlayer,
      final List<UnitGroupObservation> offense,
      final List<UnitGroupObservation> defense,
      final List<String> attackerRetreatTerritories,
      final BattleDecisionObservation decision) {
    this(
        schemaVersion,
        seed,
        battleId,
        territory,
        round,
        maxRounds,
        over,
        amphibious,
        headless,
        offensePlayer,
        defensePlayer,
        offense,
        defense,
        attackerRetreatTerritories,
        "",
        0,
        decision);
  }

  public BattleObservation {
    Objects.requireNonNull(battleId);
    Objects.requireNonNull(territory);
    Objects.requireNonNull(offensePlayer);
    Objects.requireNonNull(defensePlayer);
    offense = List.copyOf(offense);
    defense = List.copyOf(defense);
    attackerRetreatTerritories = List.copyOf(attackerRetreatTerritories);
    Objects.requireNonNull(airControlPlayer);
    Objects.requireNonNull(decision);
  }
}
