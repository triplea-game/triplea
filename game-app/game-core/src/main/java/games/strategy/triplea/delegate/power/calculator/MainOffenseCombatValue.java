package games.strategy.triplea.delegate.power.calculator;

import games.strategy.engine.data.GameSequence;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.TerritoryEffectHelper;
import games.strategy.triplea.delegate.battle.BattleState;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Value;
import org.triplea.java.collections.IntegerMap;

/**
 * Calculates offense strength and roll for non-AA dice
 *
 * <p>This takes into account marine, bombarding, territory effects, friendly support, and enemy
 * support
 */
@Builder
@Value
@Getter(AccessLevel.NONE)
class MainOffenseCombatValue implements CombatValue {

  @Nonnull GameSequence gameSequence;

  @Nonnull Integer gameDiceSides;

  @Nonnull Boolean lhtrHeavyBombers;

  @Nonnull AvailableSupports strengthSupportFromFriends;
  @Nonnull AvailableSupports strengthSupportFromEnemies;
  @Nonnull AvailableSupports rollSupportFromFriends;
  @Nonnull AvailableSupports rollSupportFromEnemies;

  @Nonnull Collection<TerritoryEffect> territoryEffects;

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
    return new MainOffenseRoll(rollSupportFromFriends.copy(), rollSupportFromEnemies.copy());
  }

  @Override
  public StrengthCalculator getStrength() {
    return new MainOffenseStrength(
        gameDiceSides,
        territoryEffects,
        strengthSupportFromFriends.copy(),
        strengthSupportFromEnemies.copy());
  }

  @Override
  public int getDiceSides(final Unit unit) {
    return gameDiceSides;
  }

  @Override
  public BattleState.Side getBattleSide() {
    return BattleState.Side.OFFENSE;
  }

  @Override
  public boolean chooseBestRoll(final Unit unit) {
    return lhtrHeavyBombers || unit.getUnitAttachment().getChooseBestRoll();
  }

  @Override
  public CombatValue buildWithNoUnitSupports() {
    return MainOffenseCombatValue.builder()
        .gameDiceSides(gameDiceSides)
        .gameSequence(gameSequence)
        .lhtrHeavyBombers(lhtrHeavyBombers)
        .rollSupportFromFriends(AvailableSupports.EMPTY_RESULT)
        .rollSupportFromEnemies(AvailableSupports.EMPTY_RESULT)
        .strengthSupportFromFriends(AvailableSupports.EMPTY_RESULT)
        .strengthSupportFromEnemies(AvailableSupports.EMPTY_RESULT)
        .friendUnits(List.of())
        .enemyUnits(List.of())
        .territoryEffects(territoryEffects)
        .build();
  }

  @Override
  public CombatValue buildOppositeCombatValue() {
    return MainDefenseCombatValue.builder()
        .gameDiceSides(gameDiceSides)
        .gameSequence(gameSequence)
        .lhtrHeavyBombers(lhtrHeavyBombers)
        .rollSupportFromFriends(rollSupportFromEnemies)
        .rollSupportFromEnemies(rollSupportFromFriends)
        .strengthSupportFromFriends(strengthSupportFromEnemies)
        .strengthSupportFromEnemies(strengthSupportFromFriends)
        .friendUnits(enemyUnits)
        .enemyUnits(friendUnits)
        .territoryEffects(territoryEffects)
        .build();
  }

  @Value
  static class MainOffenseRoll implements RollCalculator {

    AvailableSupports supportFromFriends;
    AvailableSupports supportFromEnemies;

    @Override
    public RollValue getRoll(final Unit unit) {
      return RollValue.of(unit.getUnitAttachment().getAttackRolls(unit.getOwner()))
          .add(supportFromFriends.giveSupportToUnit(unit))
          .add(supportFromEnemies.giveSupportToUnit(unit));
    }

    @Override
    public Map<Unit, IntegerMap<Unit>> getSupportGiven() {
      return SupportCalculator.getCombinedSupportsGiven(supportFromFriends, supportFromEnemies);
    }
  }

  @Value
  static class MainOffenseStrength implements StrengthCalculator {

    int gameDiceSides;
    Collection<TerritoryEffect> territoryEffects;
    AvailableSupports supportFromFriends;
    AvailableSupports supportFromEnemies;

    @Override
    public StrengthValue getStrength(final Unit unit) {
      final UnitAttachment ua = unit.getUnitAttachment();
      int strength = ua.getAttack(unit.getOwner());
      if (ua.getIsMarine() != 0 && unit.getWasAmphibious()) {
        strength += ua.getIsMarine();
      }

      return StrengthValue.of(gameDiceSides, strength)
          .add(
              TerritoryEffectHelper.getTerritoryCombatBonus(
                  unit.getType(), territoryEffects, false))
          .add(supportFromFriends.giveSupportToUnit(unit))
          .add(supportFromEnemies.giveSupportToUnit(unit));
    }

    @Override
    public Map<Unit, IntegerMap<Unit>> getSupportGiven() {
      return SupportCalculator.getCombinedSupportsGiven(supportFromFriends, supportFromEnemies);
    }
  }
}
