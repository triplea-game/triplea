package games.strategy.triplea.ui;

import games.strategy.engine.data.*;

import java.awt.event.MouseEvent;
import java.util.List;

public interface MouseOverUnitListener
{
    
    /**
     * units will be empty if the mouse is not over any unit
     */
    public void mouseEnter(List<Unit> units, Territory territory, MouseEvent me);
    
    
}
