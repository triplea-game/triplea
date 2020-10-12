package games.strategy.triplea.delegate.power.calculator;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attachments.RulesAttachment;
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
 * Calculates defense strength and roll for normal dice
 *
 * <p>This takes into account territory effects, friendly support, and enemy support
 */
@Builder
@Value
@Getter(AccessLevel.NONE)
public class NormalDefenseCalculator implements OffenseOrDefenseCalculator {

  @NonNull GameData data;
  @NonNull AvailableSupportTracker friendlySupportTracker;
  @NonNull AvailableSupportTracker enemySupportTracker;
  @NonNull Collection<TerritoryEffect> territoryEffects;

  @Override
  public StrengthOrRollCalculator getRoll() {
    return new NormalDefenseRoll(friendlySupportTracker, enemySupportTracker);
  }

  @Override
  public StrengthOrRollCalculator getStrength() {
    return new NormalDefenseStrength(
        data, friendlySupportTracker, enemySupportTracker, territoryEffects);
  }

  @Override
  public boolean isDefending() {
    return true;
  }

  @Override
  public GameData getGameData() {
    return data;
  }

  static class NormalDefenseRoll extends StrengthOrRollCalculator {

    NormalDefenseRoll(
        final AvailableSupportTracker friendlySupport, final AvailableSupportTracker enemySupport) {
      super(friendlySupport, enemySupport);
    }

    @Override
    public int getValue(final Unit unit) {
      final RollValue rollValue =
          RollValue.of(unit.getUnitAttachment().getDefenseRolls(unit.getOwner()))
              .add(addSupport(unit, friendlySupportTracker))
              .add(addSupport(unit, enemySupportTracker));
      return rollValue.getValue();
    }

    @Override
    Predicate<UnitSupportAttachment> getRuleFilter() {
      return UnitSupportAttachment::getRoll;
    }
  }

  static class NormalDefenseStrength extends StrengthOrRollCalculator {

    private final GameData gameData;
    private final Collection<TerritoryEffect> territoryEffects;

    NormalDefenseStrength(
        final GameData gameData,
        final AvailableSupportTracker friendlySupport,
        final AvailableSupportTracker enemySupport,
        final Collection<TerritoryEffect> territoryEffects) {
      super(friendlySupport, enemySupport);
      this.gameData = gameData;
      this.territoryEffects = territoryEffects;
    }

    @Override
    public int getValue(final Unit unit) {
      int strength = unit.getUnitAttachment().getDefense(unit.getOwner());
      boolean allowFriendly = true;
      if (isFirstTurnLimitedRoll(unit.getOwner())) {
        // if first turn is limited, the strength is a max of 1 and no friendly support
        strength = Math.min(1, strength);
        allowFriendly = false;
      }
      StrengthValue strengthValue =
          StrengthValue.of(gameData.getDiceSides(), strength)
              .add(
                  TerritoryEffectHelper.getTerritoryCombatBonus(
                      unit.getType(), territoryEffects, true));

      if (allowFriendly) {
        strengthValue = strengthValue.add(addSupport(unit, friendlySupportTracker));
      }
      strengthValue = strengthValue.add(addSupport(unit, enemySupportTracker));
      return strengthValue.getValue();
    }

    private boolean isFirstTurnLimitedRoll(final GamePlayer player) {
      // If player is null, Round > 1, or player has negate rule set: return false
      return !player.isNull()
          && gameData.getSequence().getRound() == 1
          && !isNegateDominatingFirstRoundAttack(player)
          && isDominatingFirstRoundAttack(gameData.getSequence().getStep().getPlayerId());
    }

    private boolean isNegateDominatingFirstRoundAttack(final GamePlayer player) {
      final RulesAttachment ra =
          (RulesAttachment) player.getAttachment(Constants.RULES_ATTACHMENT_NAME);
      return ra != null && ra.getNegateDominatingFirstRoundAttack();
    }

    private boolean isDominatingFirstRoundAttack(final GamePlayer player) {
      if (player == null) {
        return false;
      }
      final RulesAttachment ra =
          (RulesAttachment) player.getAttachment(Constants.RULES_ATTACHMENT_NAME);
      return ra != null && ra.getDominatingFirstRoundAttack();
    }

    @Override
    Predicate<UnitSupportAttachment> getRuleFilter() {
      return UnitSupportAttachment::getStrength;
    }
  }
}
