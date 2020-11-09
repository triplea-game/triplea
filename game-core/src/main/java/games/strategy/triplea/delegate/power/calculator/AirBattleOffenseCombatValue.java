package games.strategy.triplea.delegate.power.calculator;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.Properties;
import games.strategy.triplea.delegate.battle.BattleState;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Value;
import org.triplea.java.collections.IntegerMap;

/**
 * Calculates offense strength and roll for Air Battle dice
 *
 * <p>Air Battles don't have support and are not affected by territory. The unit's offense strength
 * is determined by the airAttack attribute.
 */
@Builder
@Value
@Getter(onMethod_ = @Override)
class AirBattleOffenseCombatValue implements CombatValue {

  @NonNull GameData gameData;

  @NonNull @Builder.Default Collection<Unit> friendUnits = List.of();

  @NonNull @Builder.Default Collection<Unit> enemyUnits = List.of();

  @Override
  public RollCalculator getRoll() {
    return new AirBattleOffenseRoll();
  }

  @Override
  public StrengthCalculator getStrength() {
    return new AirBattleOffenseStrength(gameData.getDiceSides());
  }

  @Override
  public BattleState.Side getBattleSide() {
    return BattleState.Side.OFFENSE;
  }

  @Override
  public int getDiceSides(final Unit unit) {
    return gameData.getDiceSides();
  }

  @Override
  public boolean chooseBestRoll(final Unit unit) {
    return Properties.getLhtrHeavyBombers(gameData) || unit.getUnitAttachment().getChooseBestRoll();
  }

  @Override
  public CombatValue buildWithNoUnitSupports() {
    return AirBattleOffenseCombatValue.builder().gameData(gameData).build();
  }

  @Override
  public CombatValue buildOppositeCombatValue() {
    return AirBattleDefenseCombatValue.builder().gameData(gameData).build();
  }

  @Value
  static class AirBattleOffenseRoll implements RollCalculator {

    @Override
    public RollValue getRoll(final Unit unit) {
      return RollValue.of(unit.getUnitAttachment().getAttackRolls(unit.getOwner()));
    }

    @Override
    public Map<Unit, IntegerMap<Unit>> getSupportGiven() {
      return Map.of();
    }
  }

  @Value
  static class AirBattleOffenseStrength implements StrengthCalculator {

    int diceSides;

    @Override
    public StrengthValue getStrength(final Unit unit) {
      return StrengthValue.of(diceSides, unit.getUnitAttachment().getAirAttack(unit.getOwner()));
    }

    @Override
    public Map<Unit, IntegerMap<Unit>> getSupportGiven() {
      return Map.of();
    }
  }
}
