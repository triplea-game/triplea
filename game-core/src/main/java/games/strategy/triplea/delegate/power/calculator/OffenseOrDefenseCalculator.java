package games.strategy.triplea.delegate.power.calculator;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.delegate.Matches;
import java.util.Collection;

public interface OffenseOrDefenseCalculator {

  StrengthOrRollCalculator getRoll();

  StrengthOrRollCalculator getStrength();

  boolean isDefending();

  GameData getGameData();

  static OffenseOrDefenseCalculator buildNormal(
      final Collection<Unit> allEnemyUnitsAliveOrWaitingToDie,
      final Collection<Unit> allFriendlyUnitsAliveOrWaitingToDie,
      final boolean defending,
      final GameData data,
      final Territory location,
      final Collection<TerritoryEffect> territoryEffects) {

    // Get all friendly supports
    final AvailableSupportTracker friendlySupportTracker =
        AvailableSupportTracker.getSortedSupport(
            allFriendlyUnitsAliveOrWaitingToDie,
            data.getUnitTypeList().getSupportRules(),
            defending,
            true);

    // Get all enemy supports
    final AvailableSupportTracker enemySupportTracker =
        AvailableSupportTracker.getSortedSupport(
            allEnemyUnitsAliveOrWaitingToDie,
            data.getUnitTypeList().getSupportRules(),
            !defending,
            false);

    return defending
        ? NormalDefenseCalculator.builder()
            .data(data)
            .friendlySupportTracker(friendlySupportTracker)
            .enemySupportTracker(enemySupportTracker)
            .territoryEffects(territoryEffects)
            .build()
        : NormalOffenseCalculator.builder()
            .data(data)
            .friendlySupportTracker(friendlySupportTracker)
            .enemySupportTracker(enemySupportTracker)
            .territoryEffects(territoryEffects)
            .territoryIsLand(Matches.territoryIsLand().test(location))
            .build();
  }

  static OffenseOrDefenseCalculator buildAa(
      final Collection<Unit> allEnemyUnitsAliveOrWaitingToDie,
      final Collection<Unit> allFriendlyUnitsAliveOrWaitingToDie,
      final boolean defending,
      final GameData data) {

    // Get all friendly supports
    final AvailableSupportTracker friendlySupportTracker =
        AvailableSupportTracker.getSortedSupport(
            allFriendlyUnitsAliveOrWaitingToDie, //
            data.getUnitTypeList().getSupportAaRules(),
            defending,
            true);

    // Get all enemy supports
    final AvailableSupportTracker enemySupportTracker =
        AvailableSupportTracker.getSortedSupport(
            allEnemyUnitsAliveOrWaitingToDie, //
            data.getUnitTypeList().getSupportAaRules(),
            !defending,
            false);

    return defending
        ? AaDefenseCalculator.builder()
            .data(data)
            .friendlySupportTracker(friendlySupportTracker)
            .enemySupportTracker(enemySupportTracker)
            .build()
        : AaOffenseCalculator.builder()
            .data(data)
            .friendlySupportTracker(friendlySupportTracker)
            .enemySupportTracker(enemySupportTracker)
            .build();
  }
}
