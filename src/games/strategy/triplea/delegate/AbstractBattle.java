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
import games.strategy.net.GUID;
import games.strategy.triplea.delegate.dataObjects.BattleRecords.BattleResult;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
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
	protected int m_round = 0;
	protected boolean m_isBombingRun;
	protected String m_battleType;
	protected boolean m_isOver = false;
	// dependent units
	// maps unit -> Collection of units
	// if unit is lost in a battle we are dependent on
	// then we lose the corresponding collection of units
	final protected Map<Unit, Collection<Unit>> m_dependentUnits = new HashMap<Unit, Collection<Unit>>();
	
	protected BattleResult m_battleResult;
	protected int m_attackerLostTUV = 0;
	protected int m_defenderLostTUV = 0;
	
	public AbstractBattle(final Territory battleSite, final PlayerID attacker, final BattleTracker battleTracker, final boolean isBombingRun, final String battleType, final GameData data)
	{
		m_battleTracker = battleTracker;
		m_attacker = attacker;
		m_battleSite = battleSite;
		m_isBombingRun = isBombingRun;
		m_battleType = battleType;
		m_data = data;
	}
	
	public Change addAttackChange(final Route route, final Collection<Unit> units, final HashMap<Unit, HashSet<Unit>> targets)
	{
		final Map<Unit, Collection<Unit>> addedTransporting = new TransportTracker().transporting(units);
		for (final Unit unit : addedTransporting.keySet())
		{
			if (m_dependentUnits.get(unit) != null)
				m_dependentUnits.get(unit).addAll(addedTransporting.get(unit));
			else
				m_dependentUnits.put(unit, addedTransporting.get(unit));
		}
		return ChangeFactory.EMPTY_CHANGE;
	}
	
	/*public Change addCombatChange(final Route route, final Collection<Unit> units, final PlayerID player)
	{
		final Map<Unit, Collection<Unit>> addedTransporting = new TransportTracker().transporting(units);
		for (Unit unit  : addedTransporting.keySet()) {
			if (m_dependentUnits.get(unit) != null)
				m_dependentUnits.get(unit).addAll(addedTransporting.get(unit));
			else
				m_dependentUnits.put(unit, addedTransporting.get(unit));
		}
		return ChangeFactory.EMPTY_CHANGE;
	}*/

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
		for (final Unit unit : units)
		{
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
		return m_round;
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
		return m_isBombingRun;
	}
	
	public String getBattleType()
	{
		return m_battleType;
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
	
	@Override
	public int hashCode()
	{
		return m_battleSite.hashCode();
	}
	
	/**
	 * 2 Battles are equal if they occur in the same territory,
	 * and are both of the same type (bombing / not-bombing),
	 * and are both of the same sub-type of bombing/normal
	 * (ex: MustFightBattle, or StrategicBombingRaidBattle, or StrategicBombingRaidPreBattle, or NonFightingBattle, etc). <br>
	 * 
	 * Equals in the sense that they should never occupy the same Set if these conditions are met.
	 */
	@Override
	public boolean equals(final Object o)
	{
		if (o == null || !(o instanceof IBattle))
			return false;
		final IBattle other = (IBattle) o;
		return other.getTerritory().equals(this.m_battleSite) && other.isBombingRun() == this.isBombingRun() && other.getBattleType().equals(this.getBattleType());
	}
	
	public abstract GUID getBattleID();
}
