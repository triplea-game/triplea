package games.strategy.triplea.ui;

import games.strategy.engine.data.Unit;

import java.awt.event.MouseEvent;
import java.util.List;

public interface UnitSelectionListener
{
    public void unitsSelected(List<Unit> units, MouseEvent me);
}
