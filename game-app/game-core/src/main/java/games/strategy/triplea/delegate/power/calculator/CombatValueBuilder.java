package games.strategy.triplea.delegate.power.calculator;

import games.strategy.engine.data.GameSequence;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.attachments.UnitSupportAttachment;
import games.strategy.triplea.delegate.battle.BattleState;
import java.util.Collection;
import lombok.Builder;
import lombok.experimental.UtilityClass;

@UtilityClass
public class CombatValueBuilder {

  @Builder(builderMethodName = "mainCombatValue", builderClassName = "MainBuilder")
  static CombatValue buildMainCombatValue(
      final Collection<Unit> enemyUnits,
      final Collection<Unit> friendlyUnits,
      final BattleState.Side side,
      final GameSequence gameSequence,
      final Collection<UnitSupportAttachment> supportAttachments,
      final boolean lhtrHeavyBombers,
      final int gameDiceSides,
      final Collection<TerritoryEffect> territoryEffects) {

    // Get all friendly supports
    final AvailableSupports supportFromFriends =
        AvailableSupports.getSortedSupport(
            new SupportCalculator(friendlyUnits, supportAttachments, side, true));

    // Get all enemy supports
    final AvailableSupports supportFromEnemies =
        AvailableSupports.getSortedSupport(
            new SupportCalculator(enemyUnits, supportAttachments, side.getOpposite(), false));

    return side == BattleState.Side.DEFENSE
        ? MainDefenseCombatValue.builder()
            .gameSequence(gameSequence)
            .gameDiceSides(gameDiceSides)
            .lhtrHeavyBombers(lhtrHeavyBombers)
            .strengthSupportFromFriends(
                supportFromFriends.filter(UnitSupportAttachment::getStrength))
            .strengthSupportFromEnemies(
                supportFromEnemies.filter(UnitSupportAttachment::getStrength))
            .rollSupportFromFriends(supportFromFriends.filter(UnitSupportAttachment::getRoll))
            .rollSupportFromEnemies(supportFromEnemies.filter(UnitSupportAttachment::getRoll))
            .friendUnits(friendlyUnits)
            .enemyUnits(enemyUnits)
            .territoryEffects(territoryEffects)
            .build()
        : MainOffenseCombatValue.builder()
            .gameSequence(gameSequence)
            .gameDiceSides(gameDiceSides)
            .lhtrHeavyBombers(lhtrHeavyBombers)
            .strengthSupportFromFriends(
                supportFromFriends.filter(UnitSupportAttachment::getStrength))
            .strengthSupportFromEnemies(
                supportFromEnemies.filter(UnitSupportAttachment::getStrength))
            .rollSupportFromFriends(supportFromFriends.filter(UnitSupportAttachment::getRoll))
            .rollSupportFromEnemies(supportFromEnemies.filter(UnitSupportAttachment::getRoll))
            .friendUnits(friendlyUnits)
            .enemyUnits(enemyUnits)
            .territoryEffects(territoryEffects)
            .build();
  }

  @Builder(builderMethodName = "aaCombatValue", builderClassName = "AaBuilder")
  static CombatValue buildAaCombatValue(
      final Collection<Unit> enemyUnits,
      final Collection<Unit> friendlyUnits,
      final BattleState.Side side,
      final Collection<UnitSupportAttachment> supportAttachments) {

    // Get all friendly supports
    final AvailableSupports supportFromFriends =
        AvailableSupports.getSortedSupport(
            new SupportCalculator(
                friendlyUnits, //
                supportAttachments,
                side,
                true));

    // Get all enemy supports
    final AvailableSupports supportFromEnemies =
        AvailableSupports.getSortedSupport(
            new SupportCalculator(
                enemyUnits, //
                supportAttachments,
                side.getOpposite(),
                false));

    return side == BattleState.Side.DEFENSE
        ? AaDefenseCombatValue.builder()
            .strengthSupportFromFriends(
                supportFromFriends.filter(UnitSupportAttachment::getAaStrength))
            .strengthSupportFromEnemies(
                supportFromEnemies.filter(UnitSupportAttachment::getAaStrength))
            .rollSupportFromFriends(supportFromFriends.filter(UnitSupportAttachment::getAaRoll))
            .rollSupportFromEnemies(supportFromEnemies.filter(UnitSupportAttachment::getAaRoll))
            .friendUnits(friendlyUnits)
            .enemyUnits(enemyUnits)
            .build()
        : AaOffenseCombatValue.builder()
            .strengthSupportFromFriends(
                supportFromFriends.filter(UnitSupportAttachment::getAaStrength))
            .strengthSupportFromEnemies(
                supportFromEnemies.filter(UnitSupportAttachment::getAaStrength))
            .rollSupportFromFriends(supportFromFriends.filter(UnitSupportAttachment::getAaRoll))
            .rollSupportFromEnemies(supportFromEnemies.filter(UnitSupportAttachment::getAaRoll))
            .friendUnits(friendlyUnits)
            .enemyUnits(enemyUnits)
            .build();
  }

  @Builder(
      builderMethodName = "navalBombardmentCombatValue",
      builderClassName = "NavalBombardmentBuilder")
  static CombatValue buildBombardmentCombatValue(
      final Collection<Unit> enemyUnits,
      final Collection<Unit> friendlyUnits,
      final Collection<UnitSupportAttachment> supportAttachments,
      final boolean lhtrHeavyBombers,
      final int gameDiceSides,
      final Collection<TerritoryEffect> territoryEffects) {

    // Get all friendly supports
    final AvailableSupports supportFromFriends =
        AvailableSupports.getSortedSupport(
            new SupportCalculator(
                friendlyUnits, supportAttachments, BattleState.Side.OFFENSE, true));

    // Get all enemy supports
    final AvailableSupports supportFromEnemies =
        AvailableSupports.getSortedSupport(
            new SupportCalculator(enemyUnits, supportAttachments, BattleState.Side.DEFENSE, false));

    return BombardmentCombatValue.builder()
        .gameDiceSides(gameDiceSides)
        .lhtrHeavyBombers(lhtrHeavyBombers)
        .strengthSupportFromFriends(supportFromFriends.filter(UnitSupportAttachment::getStrength))
        .strengthSupportFromEnemies(supportFromEnemies.filter(UnitSupportAttachment::getStrength))
        .rollSupportFromFriends(supportFromFriends.filter(UnitSupportAttachment::getRoll))
        .rollSupportFromEnemies(supportFromEnemies.filter(UnitSupportAttachment::getRoll))
        .friendUnits(friendlyUnits)
        .enemyUnits(enemyUnits)
        .territoryEffects(territoryEffects)
        .build();
  }

  @Builder(builderMethodName = "airBattleCombatValue", builderClassName = "AirBattleBuilder")
  static CombatValue buildAirBattleCombatValue(
          final Collection<Unit> enemyUnits,
          final Collection<Unit> friendlyUnits,
          final BattleState.Side side,
          final Collection<UnitSupportAttachment> supportAttachments,
          final int gameDiceSides) {

    // Get all friendly supports
    final AvailableSupports supportFromFriends =
            AvailableSupports.getSortedSupport(
                    new SupportCalculator(
                            friendlyUnits, //
                            supportAttachments,
                            side,
                            true));

    // Get all enemy supports
    final AvailableSupports supportFromEnemies =
            AvailableSupports.getSortedSupport(
                    new SupportCalculator(
                            enemyUnits, //
                            supportAttachments,
                            side.getOpposite(),
                            false));

    return side == BattleState.Side.DEFENSE
            ? AirBattleDefenseCombatValue.builder()
            .strengthSupportFromFriends(
                    supportFromFriends.filter(UnitSupportAttachment::getAirStrength))
            .strengthSupportFromEnemies(
                    supportFromEnemies.filter(UnitSupportAttachment::getAirStrength))
            .rollSupportFromFriends(supportFromFriends.filter(UnitSupportAttachment::getAirRoll))
            .rollSupportFromEnemies(supportFromEnemies.filter(UnitSupportAttachment::getAirRoll))
            .friendUnits(friendlyUnits)
            .enemyUnits(enemyUnits)
            .gameDiceSides(gameDiceSides)
            .build()
            : AirBattleOffenseCombatValue.builder()
            .strengthSupportFromFriends(
                    supportFromFriends.filter(UnitSupportAttachment::getAirStrength))
            .strengthSupportFromEnemies(
                    supportFromEnemies.filter(UnitSupportAttachment::getAirStrength))
            .rollSupportFromFriends(supportFromFriends.filter(UnitSupportAttachment::getAirRoll))
            .rollSupportFromEnemies(supportFromEnemies.filter(UnitSupportAttachment::getAirRoll))
            .friendUnits(friendlyUnits)
            .enemyUnits(enemyUnits)
            .gameDiceSides(gameDiceSides)
            .build();
  }
}
