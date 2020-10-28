package games.strategy.triplea.delegate.power.calculator;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.attachments.UnitSupportAttachment;
import games.strategy.triplea.delegate.TerritoryEffectHelper;
import java.util.Collection;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Value;
import org.triplea.java.collections.IntegerMap;

/**
 * Calculates offense strength and roll for normal dice
 *
 * <p>This takes into account marine, bombarding, territory effects, friendly support, and enemy
 * support
 */
@Builder
@Value
@Getter(AccessLevel.NONE)
class MainOffenseCombatValue implements CombatValue {

  @Getter(onMethod = @__({@Override}))
  @NonNull
  GameData gameData;

  @NonNull AvailableSupports supportFromFriends;
  @NonNull AvailableSupports supportFromEnemies;
  @NonNull Collection<TerritoryEffect> territoryEffects;
  boolean territoryIsLand;

  @Override
  public RollCalculator getRoll() {
    return new MainOffenseRoll(supportFromFriends, supportFromEnemies);
  }

  @Override
  public StrengthCalculator getStrength() {
    return new MainOffenseStrength(
        gameData, supportFromFriends, supportFromEnemies, territoryEffects, territoryIsLand);
  }

  @Override
  public boolean isDefending() {
    return false;
  }

  static class MainOffenseRoll implements RollCalculator {

    AvailableSupports supportFromFriends;
    AvailableSupports supportFromEnemies;
    StrengthAndRollCalculator calculator = new StrengthAndRollCalculator();

    MainOffenseRoll(
        final AvailableSupports supportFromFriends, final AvailableSupports supportFromEnemies) {
      this.supportFromFriends = supportFromFriends.filter(UnitSupportAttachment::getRoll);
      this.supportFromEnemies = supportFromEnemies.filter(UnitSupportAttachment::getRoll);
    }

    @Override
    public RollValue getRoll(final Unit unit) {
      return RollValue.of(unit.getUnitAttachment().getAttackRolls(unit.getOwner()))
          .add(calculator.giveSupport(unit, supportFromFriends))
          .add(calculator.giveSupport(unit, supportFromEnemies));
    }

    @Override
    public Map<Unit, IntegerMap<Unit>> getSupportGiven() {
      return calculator.getSupportGiven();
    }
  }

  static class MainOffenseStrength implements StrengthCalculator {

    private final GameData gameData;
    private final Collection<TerritoryEffect> territoryEffects;
    private final boolean territoryIsLand;
    AvailableSupports supportFromFriends;
    AvailableSupports supportFromEnemies;
    StrengthAndRollCalculator calculator = new StrengthAndRollCalculator();

    MainOffenseStrength(
        final GameData gameData,
        final AvailableSupports supportFromFriends,
        final AvailableSupports supportFromEnemies,
        final Collection<TerritoryEffect> territoryEffects,
        final boolean territoryIsLand) {
      this.gameData = gameData;
      this.territoryEffects = territoryEffects;
      this.territoryIsLand = territoryIsLand;
      this.supportFromFriends = supportFromFriends.filter(UnitSupportAttachment::getStrength);
      this.supportFromEnemies = supportFromEnemies.filter(UnitSupportAttachment::getStrength);
    }

    @Override
    public StrengthValue getStrength(final Unit unit) {
      final UnitAttachment ua = unit.getUnitAttachment();
      int strength = ua.getAttack(unit.getOwner());
      if (ua.getIsMarine() != 0 && unit.getWasAmphibious()) {
        strength += ua.getIsMarine();
      }
      if (ua.getIsSea() && territoryIsLand) {
        // Change the strength to be bombard, not attack/defense, because this is a bombarding
        // naval unit
        strength = ua.getBombard();
      }

      return StrengthValue.of(gameData.getDiceSides(), strength)
          .add(
              TerritoryEffectHelper.getTerritoryCombatBonus(
                  unit.getType(), territoryEffects, false))
          .add(calculator.giveSupport(unit, supportFromFriends))
          .add(calculator.giveSupport(unit, supportFromEnemies));
    }

    @Override
    public Map<Unit, IntegerMap<Unit>> getSupportGiven() {
      return calculator.getSupportGiven();
    }
  }
}
