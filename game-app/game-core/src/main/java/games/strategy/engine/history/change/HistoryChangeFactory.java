package games.strategy.engine.history.change;

import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.history.change.units.DamageUnitsHistoryChange;
import games.strategy.engine.history.change.units.RemoveUnitsHistoryChange;
import games.strategy.engine.history.change.units.TransformDamagedUnitsHistoryChange;
import java.util.Collection;
import lombok.experimental.UtilityClass;
import org.triplea.java.collections.IntegerMap;

@UtilityClass
public class HistoryChangeFactory {

  public TransformDamagedUnitsHistoryChange transformDamagedUnits(
      final Territory location,
      final Collection<Unit> damagedUnits,
      final boolean markNoMovementOnNewUnits) {
    return new TransformDamagedUnitsHistoryChange(location, damagedUnits, markNoMovementOnNewUnits);
  }

  public RemoveUnitsHistoryChange removeUnitsFromTerritory(
      final Territory location, final Collection<Unit> killedUnits) {
    return new RemoveUnitsHistoryChange(location, killedUnits, "${units} lost in ${territory}");
  }

  public RemoveUnitsHistoryChange removeUnitsWithAa(
      final Territory location, final Collection<Unit> killedUnits, final String aaType) {
    return new RemoveUnitsHistoryChange(
        location, killedUnits, "${units} killed by " + aaType + " in ${territory}");
  }

  public DamageUnitsHistoryChange damageUnits(
      final Territory location, final IntegerMap<Unit> damagedUnits) {
    return new DamageUnitsHistoryChange(location, damagedUnits);
  }
}
