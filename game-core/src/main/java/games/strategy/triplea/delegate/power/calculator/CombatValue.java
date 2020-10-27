package games.strategy.triplea.delegate.power.calculator;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.delegate.Matches;
import java.util.Collection;

public interface CombatValue {

  StrengthAndRollCalculator getRoll();

  StrengthAndRollCalculator getStrength();

  boolean isDefending();

  GameData getGameData();

  static CombatValue buildMain(
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
            .territoryEffects(territoryEffects)
            .build()
        : MainOffenseCombatValue.builder()
            .gameData(data)
            .supportFromFriends(supportFromFriends)
            .supportFromEnemies(supportFromEnemies)
            .territoryEffects(territoryEffects)
            .territoryIsLand(Matches.territoryIsLand().test(location))
            .build();
  }

  static CombatValue buildAa(
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
            .build()
        : AaOffenseCombatValue.builder()
            .gameData(data)
            .supportFromFriends(supportFromFriends)
            .supportFromEnemies(supportFromEnemies)
            .build();
  }
}
