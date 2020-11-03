package games.strategy.triplea.delegate.power.calculator;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
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

/** Calculates offense strength and roll for Air Battle dice */
@Builder
@Value
@Getter(AccessLevel.NONE)
class AirBattleOffenseCombatValue implements CombatValue {

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

  @Getter(onMethod = @__({@Override}))
  @NonNull
  @Builder.Default
  Collection<TerritoryEffect> territoryEffects = List.of();

  @Override
  public RollCalculator getRoll() {
    return new AirBattleOffenseRoll();
  }

  @Override
  public StrengthCalculator getStrength() {
    return new AirBattleOffenseStrength(gameData.getDiceSides());
  }

  @Override
  public boolean isDefending() {
    return false;
  }

  @Override
  public int getDiceSides(final Unit unit) {
    return gameData.getDiceSides();
  }

  @Value
  @AllArgsConstructor
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
  @AllArgsConstructor
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
