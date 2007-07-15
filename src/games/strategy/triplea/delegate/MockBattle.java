package games.strategy.triplea.delegate;

import games.strategy.engine.data.*;
import games.strategy.engine.delegate.IDelegateBridge;

import java.util.ArrayList;
import java.util.Collection;

public class MockBattle implements Battle
{

    private final Territory m_location;
    
    private Collection<Unit> m_amphibiousLandAttackers = new ArrayList<Unit>();

    private boolean m_isAmphibious;
    
    public MockBattle(Territory location)
    {
        m_location = location;
    }

    public Change addAttackChange(Route route, Collection<Unit> units)
    {
        return ChangeFactory.EMPTY_CHANGE;
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
        return m_isAmphibious;
    }

    public void setIsAmphibious(boolean aBool)
    {
        m_isAmphibious = aBool;
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
        return m_amphibiousLandAttackers;
    }
    
    public void setAmphibiousLandAttackers(Collection<Unit> units) 
    {
        m_amphibiousLandAttackers = new ArrayList<Unit>(units);
    }

    public int getBattleRound()
    {
        // TODO Auto-generated method stub
        return 0;
    }

}
