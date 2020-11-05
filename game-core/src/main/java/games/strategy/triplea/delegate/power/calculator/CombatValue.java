package games.strategy.triplea.delegate.power.calculator;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.delegate.Matches;
import java.util.Collection;

public interface CombatValue {

  RollCalculator getRoll();

  StrengthCalculator getStrength();

  int getDiceSides(Unit unit);

  boolean isDefending();

  Collection<TerritoryEffect> getTerritoryEffects();

  GameData getGameData();

  Collection<Unit> getFriendUnits();

  Collection<Unit> getEnemyUnits();

  static CombatValue buildMainCombatValue(
      final Collection<Unit> allEnemyUnitsAliveOrWaitingToDie,
      final Collection<Unit> allFriendlyUnitsAliveOrWaitingToDie,
      final boolean defending,
      final GameData data,
      final Territory location,
      final Collection<TerritoryEffect> territoryEffects) {

    // Get all friendly supports
    final AvailableSupports supportFromFriends =
        AvailableSupports.getSortedSupport(
            new SupportCalculator(
                allFriendlyUnitsAliveOrWaitingToDie,
                data.getUnitTypeList().getSupportRules(),
                defending,
                true));

    // Get all enemy supports
    final AvailableSupports supportFromEnemies =
        AvailableSupports.getSortedSupport(
            new SupportCalculator(
                allEnemyUnitsAliveOrWaitingToDie,
                data.getUnitTypeList().getSupportRules(),
                !defending,
                false));

    return defending
        ? MainDefenseCombatValue.builder()
            .gameData(data)
            .supportFromFriends(supportFromFriends)
            .supportFromEnemies(supportFromEnemies)
            .friendUnits(allFriendlyUnitsAliveOrWaitingToDie)
            .enemyUnits(allEnemyUnitsAliveOrWaitingToDie)
            .territoryEffects(territoryEffects)
            .build()
        : MainOffenseCombatValue.builder()
            .gameData(data)
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
      final GameData data) {

    // Get all friendly supports
    final AvailableSupports supportFromFriends =
        AvailableSupports.getSortedSupport(
            new SupportCalculator(
                allFriendlyUnitsAliveOrWaitingToDie, //
                data.getUnitTypeList().getSupportAaRules(),
                defending,
                true));

    // Get all enemy supports
    final AvailableSupports supportFromEnemies =
        AvailableSupports.getSortedSupport(
            new SupportCalculator(
                allEnemyUnitsAliveOrWaitingToDie, //
                data.getUnitTypeList().getSupportAaRules(),
                !defending,
                false));

    return defending
        ? AaDefenseCombatValue.builder()
            .gameData(data)
            .supportFromFriends(supportFromFriends)
            .supportFromEnemies(supportFromEnemies)
            .friendUnits(allFriendlyUnitsAliveOrWaitingToDie)
            .enemyUnits(allEnemyUnitsAliveOrWaitingToDie)
            .build()
        : AaOffenseCombatValue.builder()
            .gameData(data)
            .supportFromFriends(supportFromFriends)
            .supportFromEnemies(supportFromEnemies)
            .friendUnits(allFriendlyUnitsAliveOrWaitingToDie)
            .enemyUnits(allEnemyUnitsAliveOrWaitingToDie)
            .build();
  }
}
