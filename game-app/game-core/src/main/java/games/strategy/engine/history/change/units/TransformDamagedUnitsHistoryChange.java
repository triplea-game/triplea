package games.strategy.engine.history.change.units;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.history.change.HistoryChange;
import games.strategy.triplea.UnitUtils;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.formatter.MyFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import org.triplea.util.Tuple;

/**
 * Transforms units into other unit types as determined by {@link
 * UnitAttachment#getWhenHitPointsDamagedChangesInto()}
 */
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Getter
@EqualsAndHashCode
public class TransformDamagedUnitsHistoryChange implements HistoryChange {

  CompositeChange change = new CompositeChange();
  Territory location;

  /** Map of old unit -> new unit */
  Map<Unit, Unit> transformingUnits = new HashMap<>();

  CompositeChange attributeChanges = new CompositeChange();

  final boolean markNoMovementOnNewUnits;

  public TransformDamagedUnitsHistoryChange(
      final Territory location,
      final Collection<Unit> damagedUnits,
      final boolean markNoMovementOnNewUnits) {
    this.location = location;
    this.markNoMovementOnNewUnits = markNoMovementOnNewUnits;

    // check if each of the damaged units are supposed to change when they take damage
    // if it is supposed to change, create the new unit and translate attributes from the old unit
    // to the new unit
    for (final Unit unit : damagedUnits) {
      final Map<Integer, Tuple<Boolean, UnitType>> map =
          unit.getUnitAttachment().getWhenHitPointsDamagedChangesInto();
      if (map.containsKey(unit.getHits())) {
        final boolean translateAttributes = map.get(unit.getHits()).getFirst();
        final UnitType unitType = map.get(unit.getHits()).getSecond();
        final List<Unit> toAdd = unitType.create(1, unit.getOwner());
        if (translateAttributes) {
          attributeChanges.add(UnitUtils.translateAttributesToOtherUnits(unit, toAdd, location));
        }
        transformingUnits.put(unit, toAdd.get(0));
      }
    }
  }

  @Override
  public void perform(final IDelegateBridge bridge) {
    if (transformingUnits.isEmpty()) {
      return;
    }

    final CompositeChange compositeChange =
        new CompositeChange(
            ChangeFactory.addUnits(location, getNewUnits()),
            ChangeFactory.removeUnits(location, getOldUnits()),
            attributeChanges);
    if (markNoMovementOnNewUnits) {
      compositeChange.add(ChangeFactory.markNoMovementChange(getNewUnits()));
    }
    this.change.add(compositeChange);

    bridge.addChange(this.change);

    // to reduce the amount of history text, group the transforming units by both the original and
    // new unit type
    final Map<UnitType, Map<UnitType, GroupedUnits>> groupedByOldAndNewUnitTypes = new HashMap<>();
    transformingUnits.forEach(
        (oldUnit, newUnit) ->
            groupedByOldAndNewUnitTypes
                .computeIfAbsent(oldUnit.getType(), k -> new HashMap<>())
                .computeIfAbsent(newUnit.getType(), k -> new GroupedUnits())
                .addUnits(oldUnit, newUnit));

    groupedByOldAndNewUnitTypes.values().stream()
        .flatMap(tmp -> tmp.values().stream())
        .forEach(
            (groupedUnits) -> {
              final String transformTranscriptText =
                  MyFormatter.unitsToText(groupedUnits.getOldUnits())
                      + " transformed into "
                      + MyFormatter.unitsToText(groupedUnits.getNewUnits())
                      + " in "
                      + location.getName();
              bridge
                  .getHistoryWriter()
                  .addChildToEvent(transformTranscriptText, groupedUnits.getOldUnits());
            });
  }

  @Override
  public Change invert() {
    return this.change.invert();
  }

  @Value
  private static class GroupedUnits {
    Collection<Unit> oldUnits = new ArrayList<>();
    Collection<Unit> newUnits = new ArrayList<>();

    void addUnits(final Unit oldUnit, final Unit newUnit) {
      oldUnits.add(oldUnit);
      newUnits.add(newUnit);
    }
  }

  public Collection<Unit> getOldUnits() {
    return Collections.unmodifiableCollection(transformingUnits.keySet());
  }

  public Collection<Unit> getNewUnits() {
    return Collections.unmodifiableCollection(transformingUnits.values());
  }
}
