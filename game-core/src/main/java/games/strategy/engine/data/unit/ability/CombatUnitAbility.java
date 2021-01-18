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

  /** Will these units commit suicide when they have a hit? */
  @Builder.Default boolean suicideOnHit = false;

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
   * <p>The name is not important in deciding whether the attachedTo can be combined.
   *
   * <p>The attachedTo is only important if this is an suicideOnHit ability. By ensuring that each
   * CombatUnitAbility is unique per suicideOnHit unitType, this simplifies the logic to determine
   * which unit should suicide after a hit.
   */
  public boolean canMergeAttachedUnitTypes(final CombatUnitAbility other) {
    // build a new CombatUnitAbility based off of other but with this CombatUnitAbility's name
    // and attachedUnitTypes (if not suicideOnHit) as they are not relevant for merging.
    return equals(
        other.toBuilder()
            .name(this.name)
            .attachedUnitTypes(suicideOnHit ? other.attachedUnitTypes : this.attachedUnitTypes)
            .build());
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
