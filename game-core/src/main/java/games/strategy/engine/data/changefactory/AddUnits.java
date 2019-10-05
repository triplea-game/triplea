package games.strategy.engine.data.changefactory;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitCollection;
import games.strategy.engine.data.UnitHolder;
import java.util.ArrayList;
import java.util.Collection;

/** Add units. */
public class AddUnits extends Change {
  private static final long serialVersionUID = 2694342784633196289L;

  private final String name;
  private final Collection<Unit> units;
  private final String type;

  AddUnits(final UnitCollection collection, final Collection<Unit> units) {
    this.units = new ArrayList<>(units);
    name = collection.getHolder().getName();
    type = collection.getHolder().getType();
  }

  AddUnits(final String name, final String type, final Collection<Unit> units) {
    this.units = new ArrayList<>(units);
    this.type = type;
    this.name = name;
  }

  @Override
  public Change invert() {
    return new RemoveUnits(name, type, units);
  }

  @Override
  protected void perform(final GameData data) {
    final UnitHolder holder = data.getUnitHolder(name, type);
    holder.getUnitCollection().addAll(units);
  }

  @Override
  public String toString() {
    return "Add unit change.  Add to:" + name + " units:" + units;
  }
}
