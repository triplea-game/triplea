package games.strategy.engine.history.change.units;

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
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Value;
import org.apache.commons.text.StringSubstitutor;
import org.triplea.java.collections.IntegerMap;

/**
 * Kills a set of units in a location
 *
 * <p>Adds a history event for the killing
 *
 * <p>Transforms units to other units if needed. See {@link TransformUnits}
 */
@Value
public class KillUnits implements HistoryChange {

  Territory location;
  Collection<Unit> killedUnits;
  Map<Territory, Collection<Unit>> unloadedUnits = new HashMap<>();
  TransformUnits transformUnits;
  String messageTemplate;
  /** Units that were killed */
  Collection<Unit> oldUnits = new ArrayList<>();
  /** The units that were created after a transformation */
  Collection<Unit> newUnits = new ArrayList<>();

  public KillUnits(final Territory location, final Collection<Unit> killedUnits) {
    this(location, killedUnits, "${units} lost in ${territory}");
  }

  /** @param messageTemplate ${units} and ${territory} will be replaced in this template */
  public KillUnits(
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

    transformUnits = new TransformUnits(location, killedUnits, true);

    killedUnits.forEach(
        unit -> {
          unit.setHits(originalHits.getInt(unit));
        });

    oldUnits.addAll(killedUnits);
    // ensure that any units that are being transported are also killed
    killedUnits.stream()
        .map(unit -> unit.getTransporting(location))
        .flatMap(Collection::stream)
        .forEach(oldUnits::add);
    // any unit that was unloaded during combat phase needs to be removed but it needs to be removed
    // from the territory it unloaded to
    killedUnits.stream()
        .map(Unit::getUnloaded)
        .flatMap(Collection::stream)
        .forEach(
            unloadedUnit -> {
              unloadedUnits
                  .computeIfAbsent(unloadedUnit.getUnloadedTo(), k -> new ArrayList<>())
                  .add(unloadedUnit);
              oldUnits.add(unloadedUnit);
            });

    newUnits.addAll(transformUnits.getNewUnits());

    this.killedUnits = new ArrayList<>(oldUnits);
    // remove the units that were transformed as TransformUnits will handle them
    this.killedUnits.removeAll(transformUnits.getOldUnits());
    // remove the unloaded units as they will be handled separately
    this.killedUnits.removeAll(
        unloadedUnits.values().stream().flatMap(Collection::stream).collect(Collectors.toList()));
  }

  @Override
  public void perform(final IDelegateBridge bridge) {
    transformUnits.perform(bridge);

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
    bridge.addChange(change);

    final String text =
        new StringSubstitutor(
                Map.of(
                    "units", MyFormatter.unitsToText(allKilledUnits),
                    "territory", location.getName()))
            .replace(messageTemplate);
    bridge.getHistoryWriter().addChildToEvent(text, allKilledUnits);
  }
}
