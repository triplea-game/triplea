package games.strategy.triplea.delegate.power.calculator;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.attachments.UnitSupportAttachment;
import games.strategy.triplea.delegate.TerritoryEffectHelper;
import java.util.Collection;
import java.util.function.Predicate;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Value;

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
  public StrengthOrRollCalculator getRoll() {
    return new MainOffenseRoll(supportFromFriends, supportFromEnemies);
  }

  @Override
  public StrengthOrRollCalculator getStrength() {
    return new MainOffenseStrength(
        gameData, supportFromFriends, supportFromEnemies, territoryEffects, territoryIsLand);
  }

  @Override
  public boolean isDefending() {
    return false;
  }

  static class MainOffenseRoll extends StrengthOrRollCalculator {

    MainOffenseRoll(final AvailableSupports friendlySupport, final AvailableSupports enemySupport) {
      super(friendlySupport, enemySupport);
    }

    @Override
    public int getValue(final Unit unit) {
      return RollValue.of(unit.getUnitAttachment().getAttackRolls(unit.getOwner()))
          .add(addSupport(unit, friendlySupportTracker))
          .add(addSupport(unit, enemySupportTracker))
          .getValue();
    }

    @Override
    protected Predicate<UnitSupportAttachment> getRuleFilter() {
      return UnitSupportAttachment::getRoll;
    }
  }

  static class MainOffenseStrength extends StrengthOrRollCalculator {

    private final GameData gameData;
    private final Collection<TerritoryEffect> territoryEffects;
    private final boolean territoryIsLand;

    MainOffenseStrength(
        final GameData gameData,
        final AvailableSupports friendlySupport,
        final AvailableSupports enemySupport,
        final Collection<TerritoryEffect> territoryEffects,
        final boolean territoryIsLand) {
      super(friendlySupport, enemySupport);
      this.gameData = gameData;
      this.territoryEffects = territoryEffects;
      this.territoryIsLand = territoryIsLand;
    }

    @Override
    public int getValue(final Unit unit) {
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

      final StrengthValue strengthValue =
          StrengthValue.of(gameData.getDiceSides(), strength)
              .add(
                  TerritoryEffectHelper.getTerritoryCombatBonus(
                      unit.getType(), territoryEffects, false))
              .add(addSupport(unit, friendlySupportTracker))
              .add(addSupport(unit, enemySupportTracker));
      return strengthValue.getValue();
    }

    @Override
    protected Predicate<UnitSupportAttachment> getRuleFilter() {
      return UnitSupportAttachment::getStrength;
    }
  }
}
