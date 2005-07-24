package games.strategy.triplea.ui;

import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;

import java.awt.event.MouseEvent;
import java.util.List;

public interface UnitSelectionListener
{
    
    /**
     * Note, if the mouse is clicked where there are no units, units will be empty and territory null. 
     */
    public void unitsSelected(List<Unit> units, Territory territory, MouseEvent me);
}
