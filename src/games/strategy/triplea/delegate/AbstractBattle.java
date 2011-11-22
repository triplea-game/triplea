/**
 * Created on Oct 23, 2011
 */
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
import java.util.Iterator;
import java.util.Map;

/**
 * @author Erik von der Osten
 * 
 */
@SuppressWarnings("serial")
abstract public class AbstractBattle implements IBattle
{
	final protected Territory m_battleSite;
	final protected PlayerID m_attacker;
	final protected BattleTracker m_battleTracker;
	final protected GameData m_data;
	protected boolean m_isOver = false;
	// dependent units
	// maps unit -> Collection of units
	// if unit is lost in a battle we are dependent on
	// then we lose the corresponding collection of units
	final protected Map<Unit, Collection<Unit>> m_dependentUnits = new HashMap<Unit, Collection<Unit>>();
	
	public AbstractBattle(final Territory battleSite, final PlayerID attacker, final BattleTracker battleTracker, final GameData data)
	{
		m_battleTracker = battleTracker;
		m_attacker = attacker;
		m_battleSite = battleSite;
		m_data = data;
	}
	
	public Change addAttackChange(final Route route, final Collection<Unit> units)
	{
		final Map<Unit, Collection<Unit>> addedTransporting = new TransportTracker().transporting(units);
		final Iterator<Unit> iter = addedTransporting.keySet().iterator();
		while (iter.hasNext())
		{
			final Unit unit = iter.next();
			if (m_dependentUnits.get(unit) != null)
				m_dependentUnits.get(unit).addAll(addedTransporting.get(unit));
			else
				m_dependentUnits.put(unit, addedTransporting.get(unit));
		}
		return ChangeFactory.EMPTY_CHANGE;
	}
	
	public Change addCombatChange(final Route route, final Collection<Unit> units, final PlayerID player)
	{
		final Map<Unit, Collection<Unit>> addedTransporting = new TransportTracker().transporting(units);
		final Iterator<Unit> iter = addedTransporting.keySet().iterator();
		while (iter.hasNext())
		{
			final Unit unit = iter.next();
			if (m_dependentUnits.get(unit) != null)
				m_dependentUnits.get(unit).addAll(addedTransporting.get(unit));
			else
				m_dependentUnits.put(unit, addedTransporting.get(unit));
		}
		return ChangeFactory.EMPTY_CHANGE;
	}
	
	public final Territory getTerritory()
	{
		return m_battleSite;
	}
	
	/*
	 * (non-Javadoc)
	 * @see games.strategy.triplea.delegate.IBattle#unitsLostInPrecedingBattle(games.strategy.triplea.delegate.IBattle,java.util.Collection<Unit>,games.strategy.engine.delegate.IDelegateBridge)
	 */
	abstract public void unitsLostInPrecedingBattle(IBattle battle, Collection<Unit> units, IDelegateBridge bridge);
	
	public Collection<Unit> getDependentUnits(final Collection<Unit> units)
	{
		final Collection<Unit> rVal = new ArrayList<Unit>();
		final Iterator<Unit> iter = units.iterator();
		while (iter.hasNext())
		{
			final Unit unit = iter.next();
			final Collection<Unit> dependent = m_dependentUnits.get(unit);
			if (dependent != null)
				rVal.addAll(dependent);
		}
		return rVal;
	}
	
	/**
	 * Add bombarding unit. Doesn't make sense here so just do
	 * nothing.
	 */
	public void addBombardingUnit(final Unit unit)
	{
		// nothing
	}
	
	/**
	 * Return whether battle is amphibious.
	 */
	public boolean isAmphibious()
	{
		return false;
	}
	
	public Collection<Unit> getAmphibiousLandAttackers()
	{
		return new ArrayList<Unit>();
	}
	
	public Collection<Unit> getBombardingUnits()
	{
		return new ArrayList<Unit>();
	}
	
	public int getBattleRound()
	{
		return 0;
	}
	
	public Collection<Unit> getAttackingUnits()
	{
		return new ArrayList<Unit>();
	}
	
	public Collection<Unit> getDefendingUnits()
	{
		return new ArrayList<Unit>();
	}
	
	public final boolean isOver()
	{
		return m_isOver;
	}
	
	/*
	 * (non-Javadoc)
	 * @see games.strategy.triplea.delegate.IBattle#isBombingRun()
	 */
	public boolean isBombingRun()
	{
		return false;
	}
	
	/*
	 * (non-Javadoc)
	 * @see games.strategy.triplea.delegate.IBattle#fight(games.strategy.engine.delegate.IDelegateBridge)
	 */
	abstract public void fight(IDelegateBridge bridge);
	
	/*
	 * (non-Javadoc)
	 * @see games.strategy.triplea.delegate.IBattle#removeAttack(games.strategy.engine.data.Route, java.util.Collection)
	 */
	abstract public void removeAttack(Route route, Collection<Unit> units);
	
	/*
	 * (non-Javadoc)
	 * @see games.strategy.triplea.delegate.IBattle#isEmpty()
	 */
	public boolean isEmpty()
	{
		return false;
	}
}
