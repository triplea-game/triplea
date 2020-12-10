package games.strategy.triplea.delegate.power.calculator;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameSequence;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attachments.RulesAttachment;
import games.strategy.triplea.delegate.TerritoryEffectHelper;
import games.strategy.triplea.delegate.battle.BattleState;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
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

  @NonNull GameSequence gameSequence;

  @NonNull Integer gameDiceSides;

  @NonNull Boolean lhtrHeavyBombers;

  @NonNull AvailableSupports strengthSupportFromFriends;
  @NonNull AvailableSupports strengthSupportFromEnemies;
  @NonNull AvailableSupports rollSupportFromFriends;
  @NonNull AvailableSupports rollSupportFromEnemies;

  @NonNull Collection<TerritoryEffect> territoryEffects;

  @Getter(onMethod_ = @Override)
  @NonNull
  @Builder.Default
  Collection<Unit> friendUnits = List.of();

  @Getter(onMethod_ = @Override)
  @NonNull
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
      return Stream.of(
              supportFromFriends.getUnitsGivingSupport(),
              supportFromEnemies.getUnitsGivingSupport())
          .flatMap(map -> map.entrySet().stream())
          .collect(
              Collectors.toMap(
                  Map.Entry::getKey,
                  Map.Entry::getValue,
                  (value1, value2) -> {
                    final IntegerMap<Unit> merged = new IntegerMap<>(value1);
                    merged.add(value2);
                    return merged;
                  }));
    }
  }

  @Value
  static class MainDefenseStrength implements StrengthCalculator {

    GameSequence gameSequence;
    int gameDiceSides;
    Collection<TerritoryEffect> territoryEffects;
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
      final RulesAttachment rulesAttachment =
          (RulesAttachment) player.getAttachment(Constants.RULES_ATTACHMENT_NAME);
      return rulesAttachment != null && rulesAttachment.getNegateDominatingFirstRoundAttack();
    }

    private boolean isDominatingFirstRoundAttack(final GamePlayer player) {
      if (player == null) {
        return false;
      }
      final RulesAttachment rulesAttachment =
          (RulesAttachment) player.getAttachment(Constants.RULES_ATTACHMENT_NAME);
      return rulesAttachment != null && rulesAttachment.getDominatingFirstRoundAttack();
    }

    @Override
    public Map<Unit, IntegerMap<Unit>> getSupportGiven() {
      return Stream.of(
              supportFromFriends.getUnitsGivingSupport(),
              supportFromEnemies.getUnitsGivingSupport())
          .flatMap(map -> map.entrySet().stream())
          .collect(
              Collectors.toMap(
                  Map.Entry::getKey,
                  Map.Entry::getValue,
                  (value1, value2) -> {
                    final IntegerMap<Unit> merged = new IntegerMap<>(value1);
                    merged.add(value2);
                    return merged;
                  }));
    }
  }
}
