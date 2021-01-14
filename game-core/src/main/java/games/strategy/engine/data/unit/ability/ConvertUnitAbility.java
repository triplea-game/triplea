package games.strategy.engine.data.unit.ability;

import games.strategy.engine.data.UnitType;
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

/** Unit Ability to convert CombatUnitAbilities into other CombatUnitAbilities or into nothing */
@Data
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Builder(toBuilder = true)
public class ConvertUnitAbility {

  enum Faction {
    ALLIED,
    ENEMY
  }

  @NonNull String name;
  /** The unit types that have this ability */
  @NonFinal Collection<UnitType> attachedUnitTypes;

  /** Does this affect enemy and/or allied units */
  @Builder.Default Collection<Faction> factions = List.of(Faction.ENEMY);

  /** The unitAbility that will be converted */
  @NonNull CombatUnitAbility from;

  /** If to is null, then this just removes the "from" UnitAbility */
  CombatUnitAbility to;

  /**
   * Can the attachedTo from "other" be combined with this?
   *
   * <p>This is only used to merge auto created ConvertUnitAbilities.
   *
   * <p>The name and attachedTo is not important in deciding whether the attachedTo can be combined.
   */
  public boolean canMergeAttachedUnitTypes(final ConvertUnitAbility other) {
    // build a new ConvertUnitAbility based off of other but with this ConvertUnitAbility's name
    // and attachedUnitTypes as they are not relevant for merging.
    return equals(
        other.toBuilder().name(this.name).attachedUnitTypes(this.attachedUnitTypes).build());
  }

  /**
   * Merges the attachedUnitTypes from the unitAbility with this attachedUnitTypes
   *
   * <p>This is only used to merge auto created ConvertUnitAbilities.
   */
  public void mergeAttachedUnitTypes(final ConvertUnitAbility unitAbility) {
    this.attachedUnitTypes =
        Stream.of(this.attachedUnitTypes, unitAbility.attachedUnitTypes)
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
  }
}
