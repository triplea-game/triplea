package games.strategy.triplea.delegate.power.calculator;

import games.strategy.engine.data.Unit;
import games.strategy.triplea.delegate.battle.BattleState;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import lombok.Builder;
import lombok.Getter;
import lombok.Value;
import org.triplea.java.collections.IntegerMap;

/**
 * Calculates defense strength and roll for Air Battle dice
 *
 * <p>Air Battles don't have support and are not affected by territory. The unit's defense strength
 * is determined by the airDefense attribute.
 */
@Builder
@Value
class AirBattleDefenseCombatValue implements CombatValue {

  @Nonnull Integer gameDiceSides;

  @Nonnull AvailableSupports strengthSupportFromFriends;
  @Nonnull AvailableSupports strengthSupportFromEnemies;
  @Nonnull AvailableSupports rollSupportFromFriends;
  @Nonnull AvailableSupports rollSupportFromEnemies;

  @Getter(onMethod_ = @Override)
  @Nonnull
  @Builder.Default
  Collection<Unit> friendUnits = List.of();

  @Getter(onMethod_ = @Override)
  @Nonnull
  @Builder.Default
  Collection<Unit> enemyUnits = List.of();

  @Override
  public RollCalculator getRoll() {
    return new AirBattleDefenseRoll(rollSupportFromFriends.copy(), rollSupportFromEnemies.copy());
  }

  @Override
  public StrengthCalculator getStrength() {
    return new AirBattleDefenseStrength(
        gameDiceSides,
        strengthSupportFromFriends.copy(),
        strengthSupportFromEnemies.copy());
  }

  @Override
  public int getDiceSides(final Unit unit) {
    return gameDiceSides;
  }

  @Override
  public BattleState.Side getBattleSide() {
    return BattleState.Side.DEFENSE;
  }

  @Override
  public boolean chooseBestRoll(final Unit unit) {
    return false;
  }

  @Override
  public CombatValue buildWithNoUnitSupports() {
    return AirBattleDefenseCombatValue.builder()
        .gameDiceSides(gameDiceSides)
        .rollSupportFromFriends(AvailableSupports.EMPTY_RESULT)
        .rollSupportFromEnemies(AvailableSupports.EMPTY_RESULT)
        .strengthSupportFromFriends(AvailableSupports.EMPTY_RESULT)
        .strengthSupportFromEnemies(AvailableSupports.EMPTY_RESULT)
        .friendUnits(List.of())
        .enemyUnits(List.of())
        .build();
  }

  @Override
  public CombatValue buildOppositeCombatValue() {
    return AirBattleOffenseCombatValue.builder()
        .gameDiceSides(gameDiceSides)
        .rollSupportFromFriends(rollSupportFromEnemies)
        .rollSupportFromEnemies(rollSupportFromFriends)
        .strengthSupportFromFriends(strengthSupportFromEnemies)
        .strengthSupportFromEnemies(strengthSupportFromFriends)
        .friendUnits(enemyUnits)
        .enemyUnits(friendUnits)
        .build();
  }

  @Value
  static class AirBattleDefenseRoll implements RollCalculator {

    AvailableSupports supportFromFriends;
    AvailableSupports supportFromEnemies;

    @Override
    public RollValue getRoll(final Unit unit) {
      return RollValue.of(1)
           .add(supportFromFriends.giveSupportToUnit(unit))
           .add(supportFromEnemies.giveSupportToUnit(unit));
    }

    @Override
    public Map<Unit, IntegerMap<Unit>> getSupportGiven() {
      return SupportCalculator.getCombinedSupportGiven(supportFromFriends, supportFromEnemies);
    }
  }

  @Value
  static class AirBattleDefenseStrength implements StrengthCalculator {

    int diceSides;
    AvailableSupports supportFromFriends;
    AvailableSupports supportFromEnemies;

    @Override
    public StrengthValue getStrength(final Unit unit) {
      return StrengthValue.of(diceSides,
                      unit.getUnitAttachment().getAirDefense(unit.getOwner()))
           .add(supportFromFriends.giveSupportToUnit(unit))
           .add(supportFromEnemies.giveSupportToUnit(unit));
    }

    @Override
    public Map<Unit, IntegerMap<Unit>> getSupportGiven() {
      return SupportCalculator.getCombinedSupportGiven(supportFromFriends, supportFromEnemies);
    }
  }
}
