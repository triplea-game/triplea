package games.strategy.triplea.delegate.power.calculator;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.delegate.Matches;
import java.util.Collection;
import java.util.List;

public interface CombatValue {

  RollCalculator getRoll();

  StrengthCalculator getStrength();

  int getDiceSides(Unit unit);

  boolean isDefending();

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
      final Territory location,
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
            .territoryIsLand(Matches.territoryIsLand().test(location))
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

  static CombatValue buildNoSupportCombatValue(
      final boolean defending,
      final GameData gameData,
      final Collection<TerritoryEffect> territoryEffects) {

    return defending
        ? MainDefenseCombatValue.builder()
            .gameData(gameData)
            .supportFromFriends(AvailableSupports.EMPTY_RESULT)
            .supportFromEnemies(AvailableSupports.EMPTY_RESULT)
            .friendUnits(List.of())
            .enemyUnits(List.of())
            .territoryEffects(territoryEffects)
            .build()
        : MainOffenseCombatValue.builder()
            .gameData(gameData)
            .supportFromFriends(AvailableSupports.EMPTY_RESULT)
            .supportFromEnemies(AvailableSupports.EMPTY_RESULT)
            .friendUnits(List.of())
            .enemyUnits(List.of())
            .territoryEffects(territoryEffects)
            .territoryIsLand(false)
            .build();
  }
}
