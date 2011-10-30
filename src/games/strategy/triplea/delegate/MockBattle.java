package games.strategy.triplea.delegate;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.ChangeFactory;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IDelegateBridge;

import java.util.ArrayList;
import java.util.Collection;

@SuppressWarnings("serial")
public class MockBattle extends AbstractBattle
{
	
	private Collection<Unit> m_amphibiousLandAttackers = new ArrayList<Unit>();
	private Collection<Unit> m_bombardingUnits = new ArrayList<Unit>();
	
	private boolean m_isAmphibious;
	
	public MockBattle(Territory battleSite)
	{
		super(battleSite, null, null, null);
	}
	
	@Override
	public Change addAttackChange(Route route, Collection<Unit> units)
	{
		return ChangeFactory.EMPTY_CHANGE;
	}
	
	@Override
	public Change addCombatChange(Route route, Collection<Unit> units, PlayerID player)
	{
		return ChangeFactory.EMPTY_CHANGE;
	}
	
	@Override
	public final boolean isBombingRun()
	{
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	public void fight(IDelegateBridge bridge)
	{
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void unitsLostInPrecedingBattle(IBattle battle, Collection<Unit> units, IDelegateBridge bridge)
	{
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public boolean isAmphibious()
	{
		return m_isAmphibious;
	}
	
	public void setIsAmphibious(boolean aBool)
	{
		m_isAmphibious = aBool;
	}
	
	@Override
	public void removeAttack(Route route, Collection<Unit> units)
	{
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public boolean isEmpty()
	{
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	public Collection<Unit> getDependentUnits(Collection<Unit> units)
	{
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public Collection<Unit> getAmphibiousLandAttackers()
	{
		return m_amphibiousLandAttackers;
	}
	
	public void setAmphibiousLandAttackers(Collection<Unit> units)
	{
		m_amphibiousLandAttackers = new ArrayList<Unit>(units);
	}
	
	@Override
	public Collection<Unit> getBombardingUnits()
	{
		return m_bombardingUnits;
	}
	
	public void setBombardingUnits(Collection<Unit> units)
	{
		m_bombardingUnits = new ArrayList<Unit>(units);
	}
	
	@Override
	public int getBattleRound()
	{
		// TODO Auto-generated method stub
		return 0;
	}
	
	@Override
	public Collection<Unit> getAttackingUnits()
	{
		return new ArrayList<Unit>();
	}
	
	@Override
	public Collection<Unit> getDefendingUnits()
	{
		return new ArrayList<Unit>();
	}
	
}
