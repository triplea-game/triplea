package games.strategy.triplea.delegate;

import games.strategy.engine.data.*;
import games.strategy.engine.delegate.IDelegateBridge;

import java.util.Collection;

public class MockBattle implements Battle
{

    private final Territory m_location;
    
    public MockBattle(Territory location)
    {
        m_location = location;
    }

    public void addAttack(Route route, Collection<Unit> units)
    {
        // TODO Auto-generated method stub

    }

    public boolean isBombingRun()
    {
        // TODO Auto-generated method stub
        return false;
    }

    public Territory getTerritory()
    {
        return m_location;
    }

    public void fight(IDelegateBridge bridge)
    {
        // TODO Auto-generated method stub

    }

    public boolean isOver()
    {
        // TODO Auto-generated method stub
        return false;
    }

    public void unitsLost(Battle battle, Collection<Unit> units, IDelegateBridge bridge)
    {
        // TODO Auto-generated method stub

    }

    public void addBombardingUnit(Unit u)
    {
        // TODO Auto-generated method stub

    }

    public boolean isAmphibious()
    {
        // TODO Auto-generated method stub
        return false;
    }

    public void removeAttack(Route route, Collection<Unit> units)
    {
        // TODO Auto-generated method stub

    }

    public boolean isEmpty()
    {
        // TODO Auto-generated method stub
        return false;
    }

    public Collection<Unit> getDependentUnits(Collection<Unit> units)
    {
        // TODO Auto-generated method stub
        return null;
    }

    public Collection<Unit> getAmphibiousLandAttackers()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public int getBattleRound()
    {
        // TODO Auto-generated method stub
        return 0;
    }

}
