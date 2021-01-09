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
import java.util.List;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.triplea.java.collections.IntegerMap;

/** Sets the amount of damage that units have received in a territory */
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Getter
@EqualsAndHashCode
public class DamageUnitsHistoryChange implements HistoryChange {

  CompositeChange change = new CompositeChange();
  Territory location;
  IntegerMap<Unit> damageToUnits;

  public DamageUnitsHistoryChange(final Territory location, final IntegerMap<Unit> damagedUnits) {
    this.location = location;

    // add together the amount of new damage with the existing damage
    this.damageToUnits = new IntegerMap<>(damagedUnits);
    damageToUnits.keySet().forEach(unit -> damageToUnits.add(unit, unit.getHits()));
  }

  @Override
  public void perform(final IDelegateBridge bridge) {
    final Change damageUnitsChange = ChangeFactory.unitsHit(damageToUnits, List.of(location));

    this.change.add(damageUnitsChange);
    bridge.addChange(this.change);

    bridge
        .getHistoryWriter()
        .addChildToEvent(
            "Units damaged: " + MyFormatter.unitsToText(damageToUnits.keySet()),
            new ArrayList<>(damageToUnits.keySet()));
  }

  @Override
  public Change invert() {
    return this.change.invert();
  }
}
