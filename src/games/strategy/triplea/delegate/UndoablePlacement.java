package games.strategy.triplea.delegate;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.ChangeFactory;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IDelegateBridge;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

class UndoablePlacement extends AbstractUndoableMove
{
    final Territory m_territory;
    final AbstractPlaceDelegate m_delegate;
    PlayerID m_player;
    
    public UndoablePlacement(PlayerID player, AbstractPlaceDelegate delegate, CompositeChange change, Territory territory, Collection<Unit> units)
    {
        super(change, units);
        m_territory = territory;
        m_delegate = delegate;
        m_player = player;
    }
    
    protected final void undoSpecific(GameData data, IDelegateBridge bridge)
    {
        Map<Territory, Collection<Unit>> produced = m_delegate.getProduced();
        Collection<Unit> units = produced.get(m_territory);
        units.removeAll(getUnits());
        if (units.isEmpty())
        {
            produced.remove(m_territory);
        }
        m_delegate.setProduced(new HashMap<Territory, Collection<Unit>>(produced));
    }
    
    public final String getMoveLabel()
    {
        return m_territory.getName();
    }
    
    public final Territory getEnd()
    {
        return m_territory;
    }
    

}
