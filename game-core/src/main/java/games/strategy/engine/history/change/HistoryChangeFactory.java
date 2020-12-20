package games.strategy.engine.history.change;

import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.history.change.units.RemoveUnits;
import games.strategy.engine.history.change.units.TransformDamagedUnits;
import java.util.Collection;
import lombok.experimental.UtilityClass;

@UtilityClass
public class HistoryChangeFactory {

  public TransformDamagedUnits transformDamagedUnits(
      final Territory location, final Collection<Unit> damagedUnits) {
    return new TransformDamagedUnits(location, damagedUnits);
  }

  public RemoveUnits removeUnitsFromTerritory(
      final Territory location, final Collection<Unit> killedUnits) {
    return new RemoveUnits(location, killedUnits, "${units} lost in ${territory}");
  }

  public RemoveUnits removeUnitsWithAa(
      final Territory location, final Collection<Unit> killedUnits, final String aaType) {
    return new RemoveUnits(
        location, killedUnits, "${units} killed by " + aaType + " in ${territory}");
  }
}
