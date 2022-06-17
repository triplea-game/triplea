package games.strategy.triplea.delegate.power.calculator;

import games.strategy.engine.data.Unit;
import games.strategy.triplea.delegate.battle.BattleState;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Value;
import org.triplea.java.collections.IntegerMap;

/**
 * Calculates offense strength and roll for AA/Targeted dice
 *
 * <p>This takes into account friendly support, and enemy support
 */
@Builder
@Value
@Getter(AccessLevel.NONE)
class AaOffenseCombatValue implements CombatValue {

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
    return new AaRoll(rollSupportFromFriends.copy(), rollSupportFromEnemies.copy());
  }

  @Override
  public StrengthCalculator getStrength() {
    return new AaOffenseStrength(
        this, strengthSupportFromFriends.copy(), strengthSupportFromEnemies.copy());
  }

  @Override
  public int getDiceSides(final Unit unit) {
    return unit.getUnitAttachment().getOffensiveAttackAaMaxDieSides();
  }

  @Override
  public BattleState.Side getBattleSide() {
    return BattleState.Side.OFFENSE;
  }

  @Override
  public boolean chooseBestRoll(final Unit unit) {
    return false;
  }

  @Override
  public CombatValue buildWithNoUnitSupports() {
    return AaOffenseCombatValue.builder()
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
    return AaDefenseCombatValue.builder()
        .rollSupportFromFriends(rollSupportFromEnemies)
        .rollSupportFromEnemies(rollSupportFromFriends)
        .strengthSupportFromFriends(strengthSupportFromEnemies)
        .strengthSupportFromEnemies(strengthSupportFromFriends)
        .friendUnits(enemyUnits)
        .enemyUnits(friendUnits)
        .build();
  }

  @Value
  static class AaOffenseStrength implements StrengthCalculator {

    AaOffenseCombatValue calculator;
    AvailableSupports supportFromFriends;
    AvailableSupports supportFromEnemies;

    @Override
    public StrengthValue getStrength(final Unit unit) {
      return StrengthValue.of(
              calculator.getDiceSides(unit),
              unit.getUnitAttachment().getOffensiveAttackAa(unit.getOwner()))
          .add(supportFromFriends.giveSupportToUnit(unit))
          .add(supportFromEnemies.giveSupportToUnit(unit));
    }

    @Override
    public Map<Unit, IntegerMap<Unit>> getSupportGiven() {
      return SupportCalculator.getCombinedSupportGiven(supportFromFriends, supportFromEnemies);
    }
  }
}
