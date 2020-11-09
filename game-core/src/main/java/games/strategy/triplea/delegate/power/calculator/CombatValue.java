package games.strategy.triplea.delegate.power.calculator;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.delegate.battle.BattleState;
import java.util.Collection;

public interface CombatValue {

  RollCalculator getRoll();

  StrengthCalculator getStrength();

  default PowerCalculator getPower() {
    return new PowerCalculator(
        getGameData(), getStrength(), getRoll(), this::chooseBestRoll, this::getDiceSides);
  }

  int getDiceSides(Unit unit);

  BattleState.Side getBattleSide();

  boolean chooseBestRoll(Unit unit);

  GameData getGameData();

  Collection<Unit> getFriendUnits();

  Collection<Unit> getEnemyUnits();

  CombatValue buildWithNoUnitSupports();

  CombatValue buildOppositeCombatValue();

  static CombatValue buildMainCombatValue(
      final Collection<Unit> allEnemyUnitsAliveOrWaitingToDie,
      final Collection<Unit> allFriendlyUnitsAliveOrWaitingToDie,
      final BattleState.Side side,
      final GameData gameData,
      final Collection<TerritoryEffect> territoryEffects) {

    // Get all friendly supports
    final AvailableSupports supportFromFriends =
        AvailableSupports.getSortedSupport(
            new SupportCalculator(
                allFriendlyUnitsAliveOrWaitingToDie,
                gameData.getUnitTypeList().getSupportRules(),
                side,
                true));

    // Get all enemy supports
    final AvailableSupports supportFromEnemies =
        AvailableSupports.getSortedSupport(
            new SupportCalculator(
                allEnemyUnitsAliveOrWaitingToDie,
                gameData.getUnitTypeList().getSupportRules(),
                side.getOpposite(),
                false));

    return side == BattleState.Side.DEFENSE
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
      final BattleState.Side side,
      final GameData gameData) {

    // Get all friendly supports
    final AvailableSupports supportFromFriends =
        AvailableSupports.getSortedSupport(
            new SupportCalculator(
                allFriendlyUnitsAliveOrWaitingToDie, //
                gameData.getUnitTypeList().getSupportAaRules(),
                side,
                true));

    // Get all enemy supports
    final AvailableSupports supportFromEnemies =
        AvailableSupports.getSortedSupport(
            new SupportCalculator(
                allEnemyUnitsAliveOrWaitingToDie, //
                gameData.getUnitTypeList().getSupportAaRules(),
                side.getOpposite(),
                false));

    return side == BattleState.Side.DEFENSE
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
                BattleState.Side.OFFENSE,
                true));

    // Get all enemy supports
    final AvailableSupports supportFromEnemies =
        AvailableSupports.getSortedSupport(
            new SupportCalculator(
                allEnemyUnitsAliveOrWaitingToDie,
                gameData.getUnitTypeList().getSupportRules(),
                BattleState.Side.DEFENSE,
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

  static CombatValue buildAirBattleCombatValue(
      final BattleState.Side side, final GameData gameData) {

    return side == BattleState.Side.DEFENSE
        ? AirBattleDefenseCombatValue.builder().gameData(gameData).build()
        : AirBattleOffenseCombatValue.builder().gameData(gameData).build();
  }
}
