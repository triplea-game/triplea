package games.strategy.triplea.ui;

import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import java.util.Collection;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** A collection of units placed in a single territory. */
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class PlaceData {
  @Getter private final Collection<Unit> units;
  @Getter private final Territory at;
}
