package games.strategy.triplea.delegate;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.ChangeFactory;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IDelegateBridge;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

public class MockBattle extends AbstractBattle
{
	private static final long serialVersionUID = 6113135868274257523L;
	
	public MockBattle(final Territory battleSite)
	{
		this(battleSite, null, null, null);
	}
	
	public MockBattle(final Territory battleSite, final PlayerID attacker, final BattleTracker battleTracker, final GameData data)
	{
		super(battleSite, attacker, battleTracker, false, BattleType.MOCK_BATTLE, data);
	}
	
	@Override
	public Change addAttackChange(final Route route, final Collection<Unit> units, final HashMap<Unit, HashSet<Unit>> targets)
	{
		return ChangeFactory.EMPTY_CHANGE;
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
	
	public void setIsAmphibious(final boolean aBool)
	{
		m_isAmphibious = aBool;
	}
	
	public void setAmphibiousLandAttackers(final Collection<Unit> units)
	{
		m_amphibiousLandAttackers = new ArrayList<Unit>(units);
	}
	
	public void setBombardingUnits(final Collection<Unit> units)
	{
		m_bombardingUnits = new ArrayList<Unit>(units);
	}
}
