package games.strategy.triplea.delegate.battle.steps.fire.battlegroup;

import games.strategy.engine.data.UnitType;
import games.strategy.triplea.attachments.UnitAttachment;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Value;

/**
 * Tracks suicideOnHit unit types and other unit types so that firing squadrons can by grouped
 *
 * <p>Firing squadrons need to be grouped by their unit type if they are suicideOnHit. This is to
 * simplify the logic in deciding which units have committed suicide. If all the units were mixed
 * together, then it would be difficult to figure out if a non-suicideOnHit unit fired the shot or
 * if a suicideOnHit unit fired the shot. By forcing all of the same unit type to be together when
 * they are suicideOnHit, then any of the units are fine to commit suicide since they are all the
 * same.
 */
@Value
class SuicideAndNonSuicideUnitTypes {
  Collection<UnitType> suicide = new HashSet<>();
  Collection<UnitType> nonSuicide = new HashSet<>();

  SuicideAndNonSuicideUnitTypes(final UnitType unitType) {
    addUnitType(unitType);
  }

  SuicideAndNonSuicideUnitTypes(final Collection<UnitType> unitTypes) {
    unitTypes.forEach(this::addUnitType);
  }

  private void addUnitType(final UnitType unitType) {
    final UnitAttachment unitAttachment = UnitAttachment.get(unitType);
    if (unitAttachment.getIsSuicideOnHit()) {
      suicide.add(unitType);
    } else {
      nonSuicide.add(unitType);
    }
  }

  SuicideAndNonSuicideUnitTypes merge(final SuicideAndNonSuicideUnitTypes other) {
    suicide.addAll(other.suicide);
    nonSuicide.addAll(other.nonSuicide);
    return this;
  }

  Collection<FiringSquadron> groupFiringSquadrons(final FiringSquadron firingSquadron) {
    if (size() == 1) {
      // everything in the firing squadron is either non suicide or suicide of a single unit type
      // so no need to split it
      return List.of(firingSquadron);
    } else if (isSingleSuicideUnitTypeWithNonSuicide()) {
      return groupSingleSuicideUnitTypeWithNonSuicide(firingSquadron);
    } else {
      return groupMultipleSuicide(firingSquadron);
    }
  }

  private int size() {
    return suicide.size() + (nonSuicide.isEmpty() ? 0 : 1);
  }

  private boolean isSingleSuicideUnitTypeWithNonSuicide() {
    return size() == 2 && !nonSuicide.isEmpty();
  }

  private List<FiringSquadron> groupSingleSuicideUnitTypeWithNonSuicide(
      final FiringSquadron firingSquadron) {
    return List.of(
        firingSquadron.toBuilder()
            .firingUnits(
                firingSquadron
                    .getFiringUnits()
                    .and(
                        firingUnitFilterData ->
                            nonSuicide.contains(firingUnitFilterData.getFiringUnit().getType())))
            .build(),
        firingSquadron.toBuilder()
            .name(firingSquadron.getName() + " suicide")
            .firingUnits(
                firingSquadron
                    .getFiringUnits()
                    .and(
                        firingUnitFilterData ->
                            suicide.contains(firingUnitFilterData.getFiringUnit().getType())))
            .build());
  }

  private Collection<FiringSquadron> groupMultipleSuicide(final FiringSquadron firingSquadron) {
    final Collection<FiringSquadron> firingSquadrons =
        suicide.stream()
            .map(
                unitType ->
                    firingSquadron.toBuilder()
                        .name(firingSquadron.getName() + " suicide " + unitType.getName())
                        .firingUnits(
                            firingSquadron
                                .getFiringUnits()
                                .and(
                                    firingUnitFilterData ->
                                        firingUnitFilterData
                                            .getFiringUnit()
                                            .getType()
                                            .equals(unitType)))
                        .build())
            .collect(Collectors.toCollection(ArrayList::new));
    firingSquadrons.add(
        firingSquadron.toBuilder()
            .firingUnits(
                firingSquadron
                    .getFiringUnits()
                    .and(
                        firingUnitFilterData ->
                            nonSuicide.contains(firingUnitFilterData.getFiringUnit().getType())))
            .build());

    return firingSquadrons;
  }
}
