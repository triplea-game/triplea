package games.strategy.triplea.delegate.power.calculator;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import java.util.Collection;

public interface CombatValue {

  RollCalculator getRoll();

  StrengthCalculator getStrength();

  default PowerCalculator getPower() {
    return new PowerCalculator(
        getGameData(), getStrength(), getRoll(), this::chooseBestRoll, this::getDiceSides);
  }

  int getDiceSides(Unit unit);

  boolean isDefending();

  boolean chooseBestRoll(Unit unit);

  GameData getGameData();

  Collection<Unit> getFriendUnits();

  Collection<Unit> getEnemyUnits();

  CombatValue buildWithNoUnitSupports();

  CombatValue buildOppositeCombatValue();

  static CombatValue buildMainCombatValue(
      final Collection<Unit> allEnemyUnitsAliveOrWaitingToDie,
      final Collection<Unit> allFriendlyUnitsAliveOrWaitingToDie,
      final boolean defending,
      final GameData gameData,
      final Collection<TerritoryEffect> territoryEffects) {

    // Get all friendly supports
    final AvailableSupports supportFromFriends =
        AvailableSupports.getSortedSupport(
            new SupportCalculator(
                allFriendlyUnitsAliveOrWaitingToDie,
                gameData.getUnitTypeList().getSupportRules(),
                defending,
                true));

    // Get all enemy supports
    final AvailableSupports supportFromEnemies =
        AvailableSupports.getSortedSupport(
            new SupportCalculator(
                allEnemyUnitsAliveOrWaitingToDie,
                gameData.getUnitTypeList().getSupportRules(),
                !defending,
                false));

    return defending
        ? MainDefenseCombatValue.builder()
            .gameData(gameData)
            .supportFromFriends(supportFromFriends)
            .supportFromEnemies(supportFromEnemies)
            .friendUnits(allFriendlyUnitsAliveOrWaitingToDie)
            .enemyUnits(allEnemyUnitsAliveOrWaitingToDie)
            .territoryEffects(territoryEffects)
            .build()
        : MainOffenseCombatValue.builder()
            .gameData(gameData)
            .supportFromFriends(supportFromFriends)
            .supportFromEnemies(supportFromEnemies)
            .friendUnits(allFriendlyUnitsAliveOrWaitingToDie)
            .enemyUnits(allEnemyUnitsAliveOrWaitingToDie)
            .territoryEffects(territoryEffects)
            .build();
  }

  static CombatValue buildAaCombatValue(
      final Collection<Unit> allEnemyUnitsAliveOrWaitingToDie,
      final Collection<Unit> allFriendlyUnitsAliveOrWaitingToDie,
      final boolean defending,
      final GameData gameData) {

    // Get all friendly supports
    final AvailableSupports supportFromFriends =
        AvailableSupports.getSortedSupport(
            new SupportCalculator(
                allFriendlyUnitsAliveOrWaitingToDie, //
                gameData.getUnitTypeList().getSupportAaRules(),
                defending,
                true));

    // Get all enemy supports
    final AvailableSupports supportFromEnemies =
        AvailableSupports.getSortedSupport(
            new SupportCalculator(
                allEnemyUnitsAliveOrWaitingToDie, //
                gameData.getUnitTypeList().getSupportAaRules(),
                !defending,
                false));

    return defending
        ? AaDefenseCombatValue.builder()
            .gameData(gameData)
            .supportFromFriends(supportFromFriends)
            .supportFromEnemies(supportFromEnemies)
            .friendUnits(allFriendlyUnitsAliveOrWaitingToDie)
            .enemyUnits(allEnemyUnitsAliveOrWaitingToDie)
            .build()
        : AaOffenseCombatValue.builder()
            .gameData(gameData)
            .supportFromFriends(supportFromFriends)
            .supportFromEnemies(supportFromEnemies)
            .friendUnits(allFriendlyUnitsAliveOrWaitingToDie)
            .enemyUnits(allEnemyUnitsAliveOrWaitingToDie)
            .build();
  }

  static CombatValue buildBombardmentCombatValue(
      final Collection<Unit> allEnemyUnitsAliveOrWaitingToDie,
      final Collection<Unit> allFriendlyUnitsAliveOrWaitingToDie,
      final GameData gameData,
      final Collection<TerritoryEffect> territoryEffects) {

    // Get all friendly supports
    final AvailableSupports supportFromFriends =
        AvailableSupports.getSortedSupport(
            new SupportCalculator(
                allFriendlyUnitsAliveOrWaitingToDie,
                gameData.getUnitTypeList().getSupportRules(),
                false,
                true));

    // Get all enemy supports
    final AvailableSupports supportFromEnemies =
        AvailableSupports.getSortedSupport(
            new SupportCalculator(
                allEnemyUnitsAliveOrWaitingToDie,
                gameData.getUnitTypeList().getSupportRules(),
                true,
                false));

    return BombardmentCombatValue.builder()
        .gameData(gameData)
        .supportFromFriends(supportFromFriends)
        .supportFromEnemies(supportFromEnemies)
        .friendUnits(allFriendlyUnitsAliveOrWaitingToDie)
        .enemyUnits(allEnemyUnitsAliveOrWaitingToDie)
        .territoryEffects(territoryEffects)
        .build();
  }

  static CombatValue buildAirBattleCombatValue(final boolean defending, final GameData gameData) {

    return defending
        ? AirBattleDefenseCombatValue.builder().gameData(gameData).build()
        : AirBattleOffenseCombatValue.builder().gameData(gameData).build();
  }
}
