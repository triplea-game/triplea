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

/**
 * Calculates the value of a normal defensive dice roll
 *
 * <p>This takes into account territory effects, friendly support, and enemy support
 */
public class NormalDefenseStrength extends StrengthOrRollCalculator {

  private final GameData gameData;
  private final Collection<TerritoryEffect> territoryEffects;

  NormalDefenseStrength(
      final GameData gameData,
      final AvailableSupportCalculator friendlySupport,
      final AvailableSupportCalculator enemySupport,
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
      strengthValue =
          strengthValue.add(
              addSupport(unit, friendlySupportTracker, UnitSupportAttachment::getStrength));
    }
    strengthValue =
        strengthValue.add(
            addSupport(unit, enemySupportTracker, UnitSupportAttachment::getStrength));
    return strengthValue.minMax();
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
}
