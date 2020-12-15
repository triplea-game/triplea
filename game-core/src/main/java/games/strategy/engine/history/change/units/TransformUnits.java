package games.strategy.engine.history.change.units;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Value;
import org.triplea.util.Tuple;

/**
 * Transforms units into other unit types as determined by {@link
 * UnitAttachment#getWhenHitPointsDamagedChangesInto()}
 */
@Value
public class TransformUnits implements HistoryChange {

  Territory location;
  /** Map of old unit -> new unit */
  Map<Unit, Unit> transformingUnits = new HashMap<>();

  CompositeChange attributeChanges = new CompositeChange();

  public TransformUnits(final Territory location, final Collection<Unit> transformUnits) {
    this.location = location;

    for (final Unit unit : transformUnits) {

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

    bridge.addChange(
        new CompositeChange(
            ChangeFactory.addUnits(location, getNewUnits()),
            ChangeFactory.removeUnits(location, getOldUnits()),
            attributeChanges));

    // to reduce the amount of history text, group the transforming units by both the original and
    // new unit type
    transformingUnits.entrySet().stream()
        .collect(Collectors.groupingBy(entry -> entry.getKey().getType()))
        .values()
        .stream()
        .flatMap(Collection::stream)
        .collect(Collectors.groupingBy(entry -> entry.getValue().getType()))
        .values()
        .forEach(
            (entries) -> {
              final String transformTranscriptText =
                  MyFormatter.unitsToText(
                          entries.stream().map(Map.Entry::getKey).collect(Collectors.toList()))
                      + " transformed into "
                      + MyFormatter.unitsToText(
                          entries.stream().map(Map.Entry::getValue).collect(Collectors.toList()))
                      + " in "
                      + location.getName();
              bridge
                  .getHistoryWriter()
                  .addChildToEvent(
                      transformTranscriptText,
                      entries.stream().map(Map.Entry::getKey).collect(Collectors.toList()));
            });
  }

  public Collection<Unit> getOldUnits() {
    return new ArrayList<>(transformingUnits.keySet());
  }

  public Collection<Unit> getNewUnits() {
    return new ArrayList<>(transformingUnits.values());
  }
}
