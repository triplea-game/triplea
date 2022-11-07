package games.strategy.triplea.delegate.power.calculator;

import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.TerritoryEffectHelper;
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
 * Calculates offense strength and roll for non-AA dice
 *
 * <p>This takes into account marine, bombarding, territory effects, friendly support, and enemy
 * support
 */
@Builder
@Value
@Getter(AccessLevel.NONE)
public class BombardmentCombatValue implements CombatValue {

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
    return new MainOffenseCombatValue.MainOffenseRoll(
        rollSupportFromFriends.copy(), rollSupportFromEnemies.copy());
  }

  @Override
  public StrengthCalculator getStrength() {
    return new BombardmentStrength(
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
    return BombardmentCombatValue.builder()
        .gameDiceSides(gameDiceSides)
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
    return BombardmentCombatValue.builder()
        .gameDiceSides(gameDiceSides)
        .lhtrHeavyBombers(lhtrHeavyBombers)
        .rollSupportFromFriends(rollSupportFromEnemies)
        .rollSupportFromEnemies(rollSupportFromFriends)
        .strengthSupportFromFriends(rollSupportFromEnemies)
        .strengthSupportFromEnemies(rollSupportFromFriends)
        .friendUnits(enemyUnits)
        .enemyUnits(friendUnits)
        .territoryEffects(territoryEffects)
        .build();
  }

  @Value
  static class BombardmentStrength implements StrengthCalculator {

    int gameDiceSides;
    @Nonnull Collection<TerritoryEffect> territoryEffects;
    AvailableSupports supportFromFriends;
    AvailableSupports supportFromEnemies;

    @Override
    public StrengthValue getStrength(final Unit unit) {
      final UnitAttachment ua = unit.getUnitAttachment();
      final int strength = ua.getBombard();

      return StrengthValue.of(gameDiceSides, strength)
          .add(
              TerritoryEffectHelper.getTerritoryCombatBonus(
                  unit.getType(), territoryEffects, false))
          .add(supportFromFriends.giveSupportToUnit(unit))
          .add(supportFromEnemies.giveSupportToUnit(unit));
    }

    @Override
    public Map<Unit, IntegerMap<Unit>> getSupportGiven() {
      return SupportCalculator.getCombinedSupportGiven(supportFromFriends, supportFromEnemies);
    }
  }
}
