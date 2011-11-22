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
	
	public MockBattle(final Territory battleSite)
	{
		super(battleSite, null, null, null);
	}
	
	@Override
	public Change addAttackChange(final Route route, final Collection<Unit> units)
	{
		return ChangeFactory.EMPTY_CHANGE;
	}
	
	@Override
	public Change addCombatChange(final Route route, final Collection<Unit> units, final PlayerID player)
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
	public void fight(final IDelegateBridge bridge)
	{
		// TODO Auto-generated method stub
	}
	
	@Override
	public void unitsLostInPrecedingBattle(final IBattle battle, final Collection<Unit> units, final IDelegateBridge bridge)
	{
		// TODO Auto-generated method stub
	}
	
	@Override
	public boolean isAmphibious()
	{
		return m_isAmphibious;
	}
	
	public void setIsAmphibious(final boolean aBool)
	{
		m_isAmphibious = aBool;
	}
	
	@Override
	public void removeAttack(final Route route, final Collection<Unit> units)
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
	public Collection<Unit> getDependentUnits(final Collection<Unit> units)
	{
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public Collection<Unit> getAmphibiousLandAttackers()
	{
		return m_amphibiousLandAttackers;
	}
	
	public void setAmphibiousLandAttackers(final Collection<Unit> units)
	{
		m_amphibiousLandAttackers = new ArrayList<Unit>(units);
	}
	
	@Override
	public Collection<Unit> getBombardingUnits()
	{
		return m_bombardingUnits;
	}
	
	public void setBombardingUnits(final Collection<Unit> units)
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
