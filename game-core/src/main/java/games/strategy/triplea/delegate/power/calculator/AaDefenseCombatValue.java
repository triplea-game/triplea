package games.strategy.triplea.delegate.power.calculator;

import games.strategy.engine.data.Unit;
import games.strategy.triplea.delegate.battle.BattleState;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Value;
import org.triplea.java.collections.IntegerMap;

/**
 * Calculates defense strength and roll for AA/Targeted dice
 *
 * <p>This takes into account friendly support, and enemy support
 */
@Builder
@Value
@Getter(AccessLevel.NONE)
class AaDefenseCombatValue implements CombatValue {

  @NonNull AvailableSupports strengthSupportFromFriends;
  @NonNull AvailableSupports strengthSupportFromEnemies;
  @NonNull AvailableSupports rollSupportFromFriends;
  @NonNull AvailableSupports rollSupportFromEnemies;

  @Getter(onMethod_ = @Override)
  @NonNull
  @Builder.Default
  Collection<Unit> friendUnits = List.of();

  @Getter(onMethod_ = @Override)
  @NonNull
  @Builder.Default
  Collection<Unit> enemyUnits = List.of();

  @Override
  public RollCalculator getRoll() {
    return new AaRoll(rollSupportFromFriends.copy(), rollSupportFromEnemies.copy());
  }

  @Override
  public StrengthCalculator getStrength() {
    return new AaDefenseStrength(
        this, strengthSupportFromFriends.copy(), strengthSupportFromEnemies.copy());
  }

  @Override
  public int getDiceSides(final Unit unit) {
    return unit.getUnitAttachment().getAttackAaMaxDieSides();
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
    return AaDefenseCombatValue.builder()
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
    return AaOffenseCombatValue.builder()
        .rollSupportFromFriends(rollSupportFromEnemies)
        .rollSupportFromEnemies(rollSupportFromFriends)
        .strengthSupportFromFriends(strengthSupportFromEnemies)
        .strengthSupportFromEnemies(strengthSupportFromFriends)
        .friendUnits(enemyUnits)
        .enemyUnits(friendUnits)
        .build();
  }

  @Value
  static class AaDefenseStrength implements StrengthCalculator {

    AaDefenseCombatValue calculator;
    AvailableSupports supportFromFriends;
    AvailableSupports supportFromEnemies;

    @Override
    public StrengthValue getStrength(final Unit unit) {
      return StrengthValue.of(
              this.calculator.getDiceSides(unit),
              unit.getUnitAttachment().getAttackAa(unit.getOwner()))
          .add(supportFromFriends.giveSupportToUnit(unit))
          .add(supportFromEnemies.giveSupportToUnit(unit));
    }

    @Override
    public Map<Unit, IntegerMap<Unit>> getSupportGiven() {
      return Stream.of(
              supportFromFriends.getUnitsGivingSupport(),
              supportFromEnemies.getUnitsGivingSupport())
          .flatMap(map -> map.entrySet().stream())
          .collect(
              Collectors.toMap(
                  Map.Entry::getKey,
                  Map.Entry::getValue,
                  (value1, value2) -> {
                    final IntegerMap<Unit> merged = new IntegerMap<>(value1);
                    merged.add(value2);
                    return merged;
                  }));
    }
  }
}
