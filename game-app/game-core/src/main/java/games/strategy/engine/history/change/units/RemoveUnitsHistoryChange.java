package games.strategy.engine.history.change.units;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.history.change.HistoryChange;
import games.strategy.triplea.formatter.MyFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.apache.commons.text.StringSubstitutor;
import org.triplea.java.collections.IntegerMap;

/**
 * Removes a set of units in a location and adds a history event
 *
 * <p>Transforms units to other units if needed. See {@link TransformDamagedUnitsHistoryChange}
 */
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Getter
@EqualsAndHashCode
public class RemoveUnitsHistoryChange implements HistoryChange {

  CompositeChange change = new CompositeChange();
  Territory location;
  Collection<Unit> killedUnits;
  Map<Territory, Collection<Unit>> unloadedUnits = new HashMap<>();
  TransformDamagedUnitsHistoryChange transformDamagedUnitsHistoryChange;
  String messageTemplate;

  /** Units that were killed */
  Collection<Unit> oldUnits = new ArrayList<>();

  /** The units that were created after a transformation */
  Collection<Unit> newUnits = new ArrayList<>();

  /**
   * @param messageTemplate ${units} and ${territory} will be replaced in this template
   */
  public RemoveUnitsHistoryChange(
      final Territory location, final Collection<Unit> killedUnits, final String messageTemplate) {
    this.location = location;
    this.messageTemplate = messageTemplate;

    // temporarily give the unit maximum damage so that TransformUnits will be able to
    // recognize units that need to be transformed when they are killed
    final IntegerMap<Unit> originalHits = new IntegerMap<>();
    killedUnits.forEach(
        unit -> {
          originalHits.add(unit, unit.getHits());
          unit.setHits(unit.getUnitAttachment().getHitPoints());
        });

    transformDamagedUnitsHistoryChange =
        new TransformDamagedUnitsHistoryChange(location, killedUnits, false);

    killedUnits.forEach(unit -> unit.setHits(originalHits.getInt(unit)));

    final Collection<Unit> allUnloadedUnits = new HashSet<>();
    for (Unit u : killedUnits) {
      oldUnits.add(u);
      // ensure that any units that are being transported are also killed
      oldUnits.addAll(u.getTransporting(location));
      // any unit that was unloaded during combat phase needs to be removed but it needs to be
      // removed from the territory it unloaded to
      for (Unit unloadedUnit : u.getUnloaded()) {
        oldUnits.add(unloadedUnit);
        allUnloadedUnits.add(unloadedUnit);
        unloadedUnits
            .computeIfAbsent(unloadedUnit.getUnloadedTo(), k -> new ArrayList<>())
            .add(unloadedUnit);
      }
    }

    newUnits.addAll(transformDamagedUnitsHistoryChange.getNewUnits());

    // the killed units shouldn't contain units that were transformed as TransformUnits handles them
    // it should also not include unloaded units as they are handled separately
    final var transformedUnits = transformDamagedUnitsHistoryChange.getOldUnits();
    this.killedUnits =
        oldUnits.stream()
            .filter(Predicate.not(transformedUnits::contains))
            .filter(Predicate.not(allUnloadedUnits::contains))
            .collect(Collectors.toList());
  }

  @Override
  public void perform(final IDelegateBridge bridge) {
    transformDamagedUnitsHistoryChange.perform(bridge);

    final Collection<Unit> allKilledUnits = new ArrayList<>();

    final CompositeChange change = new CompositeChange();
    if (!killedUnits.isEmpty()) {
      allKilledUnits.addAll(killedUnits);
      change.add(ChangeFactory.removeUnits(location, killedUnits));
    }
    if (!unloadedUnits.isEmpty()) {
      unloadedUnits.forEach(
          (territory, units) -> {
            allKilledUnits.addAll(units);
            change.add(ChangeFactory.removeUnits(territory, units));
          });
    }
    if (change.isEmpty()) {
      return;
    }
    this.change.add(change);
    bridge.addChange(this.change);

    final String text =
        new StringSubstitutor(
                Map.of(
                    "units", MyFormatter.unitsToText(allKilledUnits),
                    "territory", location.getName()))
            .replace(messageTemplate);
    bridge.getHistoryWriter().addChildToEvent(text, allKilledUnits);
  }

  @Override
  public Change invert() {
    return this.change.invert();
  }
}
