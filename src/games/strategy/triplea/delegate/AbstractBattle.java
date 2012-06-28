/**
 * Created on Oct 23, 2011
 */
package games.strategy.triplea.delegate;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.net.GUID;
import games.strategy.triplea.TripleA;
import games.strategy.triplea.ai.weakAI.WeakAI;
import games.strategy.triplea.attatchments.UnitAttachment;
import games.strategy.triplea.delegate.dataObjects.BattleRecord.BattleResultDescription;
import games.strategy.triplea.player.ITripleaPlayer;
import games.strategy.triplea.ui.display.ITripleaDisplay;
import games.strategy.util.IntegerMap;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * @author abstraction done by Erik von der Osten & Chris Duncan
 * 
 */
abstract public class AbstractBattle implements IBattle, Serializable
{
	private static final long serialVersionUID = 871090498661731337L;
	protected final GUID m_battleID = new GUID();
	/** In headless mode we should NOT access any Delegates. In headless mode we are just being used to calculate results for an odds calculator so we can skip some steps for efficiency. */
	protected boolean m_headless = false;
	protected final Territory m_battleSite;
	protected final PlayerID m_attacker;
	protected PlayerID m_defender;
	protected final BattleTracker m_battleTracker;
	protected final GameData m_data;
	protected int m_round = 0;
	protected final boolean m_isBombingRun;
	protected boolean m_isAmphibious = false;
	protected BattleType m_battleType;
	protected boolean m_isOver = false;
	/** Dependent units, maps unit -> Collection of units, if unit is lost in a battle we are dependent on then we lose the corresponding collection of units. */
	protected final Map<Unit, Collection<Unit>> m_dependentUnits = new HashMap<Unit, Collection<Unit>>();
	protected List<Unit> m_attackingUnits = new ArrayList<Unit>();
	protected List<Unit> m_defendingUnits = new ArrayList<Unit>();
	protected List<Unit> m_amphibiousLandAttackers = new ArrayList<Unit>();
	protected List<Unit> m_bombardingUnits = new ArrayList<Unit>();
	protected Collection<TerritoryEffect> m_territoryEffects;
	
	protected BattleResultDescription m_battleResultDescription;
	protected WhoWon m_whoWon = WhoWon.NOTFINISHED;
	protected int m_attackerLostTUV = 0;
	protected int m_defenderLostTUV = 0;
	
	public AbstractBattle(final Territory battleSite, final PlayerID attacker, final BattleTracker battleTracker, final boolean isBombingRun, final BattleType battleType, final GameData data)
	{
		m_battleTracker = battleTracker;
		m_attacker = attacker;
		m_battleSite = battleSite;
		m_territoryEffects = TerritoryEffectHelper.getEffects(battleSite);
		m_isBombingRun = isBombingRun;
		m_battleType = battleType;
		m_data = data;
		m_defender = findDefender(battleSite, attacker, data);
		// Make sure that if any of the incoming data is null, we are still OK (tests and mockbattle use null for a lot of this stuff)
	}
	
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
	
	public void addBombardingUnit(final Unit unit)
	{
		m_bombardingUnits.add(unit);
	}
	
	public Collection<Unit> getBombardingUnits()
	{
		return new ArrayList<Unit>(m_bombardingUnits);
	}
	
	public boolean isAmphibious()
	{
		return m_isAmphibious;
	}
	
	public Collection<Unit> getAmphibiousLandAttackers()
	{
		return new ArrayList<Unit>(m_amphibiousLandAttackers);
	}
	
	public Collection<Unit> getAttackingUnits()
	{
		return new ArrayList<Unit>(m_attackingUnits);
	}
	
	public Collection<Unit> getDefendingUnits()
	{
		return new ArrayList<Unit>(m_defendingUnits);
	}
	
	public List<Unit> getRemainingAttackingUnits()
	{
		return new ArrayList<Unit>(m_attackingUnits);
	}
	
	public List<Unit> getRemainingDefendingUnits()
	{
		return new ArrayList<Unit>(m_defendingUnits);
	}
	
	/*
	 * (non-Javadoc)
	 * @see games.strategy.triplea.delegate.IBattle#isEmpty()
	 */
	abstract public boolean isEmpty();
	
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
	
	public BattleType getBattleType()
	{
		return m_battleType;
	}
	
	public int getBattleRound()
	{
		return m_round;
	}
	
	public WhoWon getWhoWon()
	{
		return m_whoWon;
	}
	
	public BattleResultDescription getBattleResultDescription()
	{
		return m_battleResultDescription;
	}
	
	public GUID getBattleID()
	{
		return m_battleID;
	}
	
	public final Territory getTerritory()
	{
		return m_battleSite;
	}
	
	public PlayerID getAttacker()
	{
		return m_attacker;
	}
	
	public PlayerID getDefender()
	{
		return m_defender;
	}
	
	public void setHeadless(final boolean aBool)
	{
		m_headless = aBool;
	}
	
	/*
	 * (non-Javadoc)
	 * @see games.strategy.triplea.delegate.IBattle#fight(games.strategy.engine.delegate.IDelegateBridge)
	 */
	abstract public void fight(IDelegateBridge bridge);
	
	/*
	 * (non-Javadoc)
	 * @see games.strategy.triplea.delegate.IBattle#addAttackChange(games.strategy.engine.data.Route, java.util.Collection, java.util.HashMap)
	 */
	abstract public Change addAttackChange(final Route route, final Collection<Unit> units, final HashMap<Unit, HashSet<Unit>> targets);
	
	/*
	 * (non-Javadoc)
	 * @see games.strategy.triplea.delegate.IBattle#removeAttack(games.strategy.engine.data.Route, java.util.Collection)
	 */
	abstract public void removeAttack(Route route, Collection<Unit> units);
	
	/*
	 * (non-Javadoc)
	 * @see games.strategy.triplea.delegate.IBattle#unitsLostInPrecedingBattle(games.strategy.triplea.delegate.IBattle,java.util.Collection<Unit>,games.strategy.engine.delegate.IDelegateBridge)
	 */
	abstract public void unitsLostInPrecedingBattle(IBattle battle, Collection<Unit> units, IDelegateBridge bridge);
	
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
	
	@Override
	public String toString()
	{
		return "Battle in:" + m_battleSite + " battle type:" + m_battleType + " defender:" + m_defender.getName() + " attacked by:" + m_attacker.getName() + " attacking with: " + m_attackingUnits;
	}
	
	public static PlayerID findDefender(final Territory battleSite, final PlayerID attacker, final GameData data)
	{
		if (battleSite == null)
			return PlayerID.NULL_PLAYERID;
		PlayerID defender = null;
		if (!battleSite.isWater())
		{
			defender = battleSite.getOwner();
		}
		if (data == null || attacker == null)
		{
			// This is needed for many TESTs, so do not delete
			if (defender == null)
				return PlayerID.NULL_PLAYERID;
			return defender;
		}
		if (defender == null || battleSite.isWater() || !data.getRelationshipTracker().isAtWar(attacker, defender))
		{
			// if water find the defender based on who has the most units in the territory
			final IntegerMap<PlayerID> players = battleSite.getUnits().getPlayerUnitCounts();
			int max = -1;
			for (final PlayerID current : players.keySet())
			{
				if (current.equals(attacker) || !data.getRelationshipTracker().isAtWar(attacker, current))
					continue;
				final int count = players.getInt(current);
				if (count > max)
				{
					max = count;
					defender = current;
				}
			}
			/*if (max == -1)
			{
				// this is ok, we are a headless battle
			}*/
		}
		if (defender == null)
			return PlayerID.NULL_PLAYERID;
		return defender;
	}
	
	/** The maximum number of hits that this collection of units can sustain, taking into account units with two hits, and accounting for existing damage. */
	public static int getMaxHits(final Collection<Unit> units)
	{
		int count = 0;
		for (final Unit unit : units)
		{
			if (UnitAttachment.get(unit.getUnitType()).getIsTwoHit())
			{
				count += 2;
				count -= unit.getHits();
			}
			else
			{
				count++;
			}
		}
		return count;
	}
	
	protected static TransportTracker getTransportTracker()
	{
		return new TransportTracker();
	}
	
	protected static ITripleaDisplay getDisplay(final IDelegateBridge bridge)
	{
		return (ITripleaDisplay) bridge.getDisplayChannelBroadcaster();
	}
	
	protected static ITripleaPlayer getRemote(final IDelegateBridge bridge)
	{
		return (ITripleaPlayer) bridge.getRemote();
	}
	
	protected static ITripleaPlayer getRemote(final PlayerID player, final IDelegateBridge bridge)
	{
		// if its the null player, return a do nothing proxy
		if (player.isNull())
			return new WeakAI(player.getName(), TripleA.WEAK_COMPUTER_PLAYER_TYPE);
		return (ITripleaPlayer) bridge.getRemote(player);
	}
}
