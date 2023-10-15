package games.strategy.triplea.delegate.power.calculator;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameSequence;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.attachments.RulesAttachment;
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
 * Calculates defense strength and roll for non-AA dice
 *
 * <p>This takes into account territory effects, friendly support, and enemy support
 */
@Builder
@Value
@Getter(AccessLevel.NONE)
class MainDefenseCombatValue implements CombatValue {

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
    return new MainDefenseRoll(rollSupportFromFriends.copy(), rollSupportFromEnemies.copy());
  }

  @Override
  public StrengthCalculator getStrength() {
    return new MainDefenseStrength(
        gameSequence,
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
    return BattleState.Side.DEFENSE;
  }

  @Override
  public boolean chooseBestRoll(final Unit unit) {
    return lhtrHeavyBombers || unit.getUnitAttachment().getChooseBestRoll();
  }

  @Override
  public CombatValue buildWithNoUnitSupports() {
    return MainDefenseCombatValue.builder()
        .gameSequence(gameSequence)
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
    return MainOffenseCombatValue.builder()
        .gameSequence(gameSequence)
        .gameDiceSides(gameDiceSides)
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
  static class MainDefenseRoll implements RollCalculator {

    AvailableSupports supportFromFriends;
    AvailableSupports supportFromEnemies;

    @Override
    public RollValue getRoll(final Unit unit) {
      return RollValue.of(unit.getUnitAttachment().getDefenseRolls(unit.getOwner()))
          .add(supportFromFriends.giveSupportToUnit(unit))
          .add(supportFromEnemies.giveSupportToUnit(unit));
    }

    @Override
    public Map<Unit, IntegerMap<Unit>> getSupportGiven() {
      return SupportCalculator.getCombinedSupportGiven(supportFromFriends, supportFromEnemies);
    }
  }

  @Value
  static class MainDefenseStrength implements StrengthCalculator {

    GameSequence gameSequence;
    int gameDiceSides;
    @Nonnull Collection<TerritoryEffect> territoryEffects;
    AvailableSupports supportFromFriends;
    AvailableSupports supportFromEnemies;

    @Override
    public StrengthValue getStrength(final Unit unit) {
      int strength = unit.getUnitAttachment().getDefense(unit.getOwner());
      boolean allowFriendly = true;
      if (isFirstTurnLimitedRoll(unit.getOwner())) {
        // if first turn is limited, the strength is a max of 1 and no friendly support
        strength = Math.min(1, strength);
        allowFriendly = false;
      }
      StrengthValue strengthValue =
          StrengthValue.of(gameDiceSides, strength)
              .add(
                  TerritoryEffectHelper.getTerritoryCombatBonus(
                      unit.getType(), territoryEffects, true));

      if (allowFriendly) {
        strengthValue = strengthValue.add(supportFromFriends.giveSupportToUnit(unit));
      }
      strengthValue = strengthValue.add(supportFromEnemies.giveSupportToUnit(unit));
      return strengthValue;
    }

    private boolean isFirstTurnLimitedRoll(final GamePlayer player) {
      // If player is null, Round > 1, or player has negate rule set: return false
      return !player.isNull()
          && gameSequence.getRound() == 1
          && !isNegateDominatingFirstRoundAttack(player)
          && isDominatingFirstRoundAttack(gameSequence.getStep().getPlayerId());
    }

    private boolean isNegateDominatingFirstRoundAttack(final GamePlayer player) {
      final RulesAttachment ra = player.getRulesAttachment();
      return ra != null && ra.getNegateDominatingFirstRoundAttack();
    }

    private boolean isDominatingFirstRoundAttack(final GamePlayer player) {
      if (player == null) {
        return false;
      }
      final RulesAttachment ra = player.getRulesAttachment();
      return ra != null && ra.getDominatingFirstRoundAttack();
    }

    @Override
    public Map<Unit, IntegerMap<Unit>> getSupportGiven() {
      return SupportCalculator.getCombinedSupportGiven(supportFromFriends, supportFromEnemies);
    }
  }
}
