package games.strategy.engine.data.unit.ability;

import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.delegate.battle.BattleState;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;

/** Unit Ability for combat situations */
@Data
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Builder(toBuilder = true)
public class CombatUnitAbility {

  enum DiceType {
    // Use AA dice (attackAA, offensiveAttackAA)
    AA,
    // Use bombard dice (bombard or attack if bombard is empty)
    BOMBARD,
    // Use normal dice (attack, defense)
    NORMAL
  }

  /** The name of this unit ability that will be shown in the Battle UI */
  @NonNull String name;

  /** The unit types that have this ability */
  @NonFinal Collection<UnitType> attachedUnitTypes;

  /** The unit types that can be targeted */
  @NonNull Collection<UnitType> targets;

  /** The type of dice that will be rolled. */
  @Builder.Default DiceType diceType = DiceType.NORMAL;

  /** The side(s) that the unit needs to be on to use the ability */
  @Builder.Default
  Collection<BattleState.Side> sides = List.of(BattleState.Side.OFFENSE, BattleState.Side.DEFENSE);

  /** How many combat rounds this ability can be used */
  @Builder.Default int round = Integer.MAX_VALUE;

  /**
   * Can the casualties return fire after the battle phase that this UnitAbility is active?
   *
   * <p>Casualties that are created in the battle phase where this UnitAbility is active can always
   * return fire since they are firing simultaneously
   */
  @Builder.Default boolean returnFire = true;

  /**
   * Does this unit commit suicide after it has fired a shot?
   *
   * <p>Whether the shot hit or not doesn't matter.
   *
   * <p>It can commit suicide as a defensive unit, offensive unit, or both.
   */
  @Builder.Default Collection<BattleState.Side> commitSuicide = List.of();

  /**
   * Does this unit commit suicide after it has successfully hit a target?
   *
   * <p>It can commit suicide as a defensive unit, offensive unit, or both.
   */
  @Builder.Default Collection<BattleState.Side> commitSuicideAfterSuccessfulHit = List.of();

  public boolean isTarget(final Unit unit) {
    return targets.contains(unit.getType());
  }

  public boolean isSide(final BattleState.Side side) {
    return sides.contains(side);
  }

  public boolean isRound(final int round) {
    return this.round == -1 || this.round <= round;
  }

  /**
   * Can the attachedTo from "other" be combined with this?
   *
   * <p>This is only used to merge auto created CombatUnitAbilities.
   *
   * <p>CombatUnitAbility with different names and attachedUnitTypes can still be merged together so
   * the equals need to ignore those two attributes. But equals normally still needs to check the
   * equality of names and attachedUnitTypes so don't modify the equals method itself.
   */
  public boolean canMergeAttachedUnitTypes(final CombatUnitAbility other) {
    return equals(
        other.toBuilder().name(this.name).attachedUnitTypes(this.attachedUnitTypes).build());
  }

  /**
   * Merges the attachedUnitTypes from the unitAbility with this attachedUnitTypes
   *
   * <p>This is only used to merge auto created CombatUnitAbilities.
   */
  public void mergeAttachedUnitTypes(final CombatUnitAbility unitAbility) {
    this.attachedUnitTypes =
        Stream.of(this.attachedUnitTypes, unitAbility.attachedUnitTypes)
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
  }
}
