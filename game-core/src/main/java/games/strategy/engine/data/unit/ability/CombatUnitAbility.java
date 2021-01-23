package games.strategy.engine.data.unit.ability;

import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.delegate.battle.BattleState;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;

/**
 * Unit Ability for targeting and causing damage to units
 *
 * <p>A unit ability indicates what targets a unit type has, what dice it should roll, whether its
 * casualties can return fire in a later {@link
 * games.strategy.engine.data.battle.phase.BattlePhase}.
 *
 * <p>Some examples of what this does cover are:
 *
 * <ul>
 *   <li>Instant Kill for a Submarine: It would use the NORMAL dice type, its targets would be sea
 *       units, and casualties would not return fire.
 *   <li>Land Mines Explosion: It would use the NORMAL dice type, its targets would be land based,
 *       it would commit suicide, and the casualties would return fire.
 *   <li>Depth Charges for Destroyer: It would use the AA dice type, its targets would be
 *       submarines, and the casualties would return fire.
 * </ul>
 *
 * Some examples of what this doesn't cover are:
 *
 * <ul>
 *   <li>The ability to blitz
 *   <li>The ability to retreat during a battle
 *   <li>The ability to create new units
 * </ul>
 */
@Data
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Builder(toBuilder = true)
public class CombatUnitAbility {

  public static final CombatUnitAbility EMPTY = CombatUnitAbility.builder().name("Empty").build();

  enum Suicide {
    NONE,
    /** Commit suicide after the units fire? */
    AFTER_FIRE,
    /** Commit suicide after the units actually hit a target? */
    AFTER_HIT,
  }

  /** The name of this unit ability that will be shown in the Battle UI */
  @Nonnull String name;

  /** The unit types that have this ability */
  @Builder.Default @NonFinal Collection<UnitType> attachedUnitTypes = List.of();

  /** The unit types that can be targeted */
  @Builder.Default Collection<UnitType> targets = List.of();

  /** The type of dice that will be rolled. */
  @Builder.Default CombatValueType combatValueType = CombatValueType.NORMAL;

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

  /** When should this unit commit suicide on offense */
  @Builder.Default Suicide suicideOnOffense = Suicide.NONE;
  /** When should this unit commit suicide on defense */
  @Builder.Default Suicide suicideOnDefense = Suicide.NONE;

  /** Which {@link games.strategy.triplea.delegate.power.calculator.CombatValue} should be used */
  @Value
  static class CombatValueType {
    static final CombatValueType AA = new CombatValueType("AA");
    static final CombatValueType BOMBARD = new CombatValueType("BOMBARD");
    static final CombatValueType NORMAL = new CombatValueType("NORMAL");

    String type;
  }

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
