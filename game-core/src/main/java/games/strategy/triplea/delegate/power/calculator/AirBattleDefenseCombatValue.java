package games.strategy.triplea.delegate.power.calculator;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.Properties;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
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
@Getter(AccessLevel.NONE)
class AirBattleDefenseCombatValue implements CombatValue {

  @Getter(onMethod = @__({@Override}))
  @NonNull
  GameData gameData;

  @Getter(onMethod = @__({@Override}))
  @NonNull
  @Builder.Default
  Collection<Unit> friendUnits = List.of();

  @Getter(onMethod = @__({@Override}))
  @NonNull
  @Builder.Default
  Collection<Unit> enemyUnits = List.of();

  @Override
  public RollCalculator getRoll() {
    return new AirBattleDefenseRoll();
  }

  @Override
  public StrengthCalculator getStrength() {
    return new AirBattleDefenseStrength(gameData.getDiceSides());
  }

  @Override
  public boolean isDefending() {
    return false;
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
    return AirBattleDefenseCombatValue.builder().gameData(gameData).build();
  }

  @Override
  public CombatValue buildOppositeCombatValue() {
    return AirBattleOffenseCombatValue.builder().gameData(gameData).build();
  }

  @Value
  @AllArgsConstructor
  static class AirBattleDefenseRoll implements RollCalculator {

    @Override
    public RollValue getRoll(final Unit unit) {
      return RollValue.of(unit.getUnitAttachment().getDefenseRolls(unit.getOwner()));
    }

    @Override
    public Map<Unit, IntegerMap<Unit>> getSupportGiven() {
      return Map.of();
    }
  }

  @Value
  @AllArgsConstructor
  static class AirBattleDefenseStrength implements StrengthCalculator {

    int diceSides;

    @Override
    public StrengthValue getStrength(final Unit unit) {
      return StrengthValue.of(diceSides, unit.getUnitAttachment().getAirDefense(unit.getOwner()));
    }

    @Override
    public Map<Unit, IntegerMap<Unit>> getSupportGiven() {
      return Map.of();
    }
  }
}
