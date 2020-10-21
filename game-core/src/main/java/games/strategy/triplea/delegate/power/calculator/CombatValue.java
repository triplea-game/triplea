package games.strategy.triplea.delegate.power.calculator;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.delegate.Matches;
import java.util.Collection;

public interface CombatValue {

  StrengthOrRollCalculator getRoll();

  StrengthOrRollCalculator getStrength();

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
    final AvailableSupports friendlySupportTracker =
        AvailableSupports.getSortedSupport(
            new SupportCalculator(
                allFriendlyUnitsAliveOrWaitingToDie,
                data.getUnitTypeList().getSupportRules(),
                defending,
                true));

    // Get all enemy supports
    final AvailableSupports enemySupportTracker =
        AvailableSupports.getSortedSupport(
            new SupportCalculator(
                allEnemyUnitsAliveOrWaitingToDie,
                data.getUnitTypeList().getSupportRules(),
                !defending,
                false));

    return defending
        ? MainDefenseCombatValue.builder()
            .data(data)
            .friendlySupportTracker(friendlySupportTracker)
            .enemySupportTracker(enemySupportTracker)
            .territoryEffects(territoryEffects)
            .build()
        : MainOffenseCombatValue.builder()
            .data(data)
            .friendlySupportTracker(friendlySupportTracker)
            .enemySupportTracker(enemySupportTracker)
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
    final AvailableSupports friendlySupportTracker =
        AvailableSupports.getSortedSupport(
            new SupportCalculator(
                allFriendlyUnitsAliveOrWaitingToDie, //
                data.getUnitTypeList().getSupportAaRules(),
                defending,
                true));

    // Get all enemy supports
    final AvailableSupports enemySupportTracker =
        AvailableSupports.getSortedSupport(
            new SupportCalculator(
                allEnemyUnitsAliveOrWaitingToDie, //
                data.getUnitTypeList().getSupportAaRules(),
                !defending,
                false));

    return defending
        ? AaDefenseCombatValue.builder()
            .data(data)
            .friendlySupportTracker(friendlySupportTracker)
            .enemySupportTracker(enemySupportTracker)
            .build()
        : AaOffenseCombatValue.builder()
            .data(data)
            .friendlySupportTracker(friendlySupportTracker)
            .enemySupportTracker(enemySupportTracker)
            .build();
  }
}
