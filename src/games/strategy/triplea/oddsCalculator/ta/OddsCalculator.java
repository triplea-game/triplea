package games.strategy.triplea.oddsCalculator.ta;

import games.strategy.common.delegate.GameDelegateBridge;
import games.strategy.engine.data.Change;
import games.strategy.engine.data.ChangeFactory;
import games.strategy.engine.data.ChangePerformer;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitHitsChange;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.display.IDisplay;
import games.strategy.engine.framework.GameDataUtils;
import games.strategy.engine.framework.IGameModifiedChannel;
import games.strategy.engine.gamePlayer.IRemotePlayer;
import games.strategy.engine.history.DelegateHistoryWriter;
import games.strategy.engine.history.IDelegateHistoryWriter;
import games.strategy.engine.random.IRandomStats.DiceType;
import games.strategy.engine.random.PlainRandomSource;
import games.strategy.net.GUID;
import games.strategy.triplea.ai.AIUtils;
import games.strategy.triplea.ai.AbstractAI;
import games.strategy.triplea.delegate.BattleCalculator;
import games.strategy.triplea.delegate.BattleTracker;
import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.MustFightBattle;
import games.strategy.triplea.delegate.dataObjects.CasualtyDetails;
import games.strategy.triplea.delegate.dataObjects.CasualtyList;
import games.strategy.triplea.delegate.remote.IAbstractPlaceDelegate;
import games.strategy.triplea.delegate.remote.IMoveDelegate;
import games.strategy.triplea.delegate.remote.IPurchaseDelegate;
import games.strategy.triplea.delegate.remote.ITechDelegate;
import games.strategy.triplea.ui.display.DummyDisplay;
import games.strategy.util.CompositeMatch;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.Match;
import games.strategy.util.Tuple;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class OddsCalculator
{
	private PlayerID m_attacker;
	private PlayerID m_defender;
	private GameData m_data;
	private Territory m_location;
	private Collection<Unit> m_attackingUnits = new ArrayList<Unit>();
	private Collection<Unit> m_defendingUnits = new ArrayList<Unit>();
	private Collection<Unit> m_bombardingUnits = new ArrayList<Unit>();
	private Collection<TerritoryEffect> m_territoryEffects = new ArrayList<TerritoryEffect>();
	private boolean m_keepOneAttackingLandUnit = false;
	private boolean m_amphibious = false;
	private volatile boolean m_cancelled = false;
	private int m_retreatAfterRound = Integer.MAX_VALUE;
	
	public OddsCalculator()
	{
	}
	
	@SuppressWarnings("unchecked")
	public AggregateResults calculate(final GameData data, final PlayerID attacker, final PlayerID defender, final Territory location, final Collection<Unit> attacking,
				final Collection<Unit> defending, final Collection<Unit> bombarding, final Collection<TerritoryEffect> territoryEffects, final int runCount)
	{
		m_data = GameDataUtils.cloneGameData(data, false);
		m_attacker = m_data.getPlayerList().getPlayerID(attacker.getName());
		m_defender = m_data.getPlayerList().getPlayerID(defender.getName());
		m_location = m_data.getMap().getTerritory(location.getName());
		m_territoryEffects = territoryEffects;
		m_attackingUnits = (Collection<Unit>) GameDataUtils.translateIntoOtherGameData(attacking, m_data);
		m_defendingUnits = (Collection<Unit>) GameDataUtils.translateIntoOtherGameData(defending, m_data);
		m_bombardingUnits = (Collection<Unit>) GameDataUtils.translateIntoOtherGameData(bombarding, m_data);
		final ChangePerformer changePerformer = new ChangePerformer(m_data);
		changePerformer.perform(ChangeFactory.removeUnits(m_location, m_location.getUnits().getUnits()));
		changePerformer.perform(ChangeFactory.addUnits(m_location, m_attackingUnits));
		changePerformer.perform(ChangeFactory.addUnits(m_location, m_defendingUnits));
		return calculate(runCount);
	}
	
	public void setKeepOneAttackingLandUnit(final boolean aBool)
	{
		m_keepOneAttackingLandUnit = aBool;
	}
	
	public void setAmphibious(final boolean aBool)
	{
		m_amphibious = aBool;
	}
	
	public void setRetreatAfterRound(final int value)
	{
		m_retreatAfterRound = value;
	}
	
	private AggregateResults calculate(final int count)
	{
		final long start = System.currentTimeMillis();
		// just say we are attacking from all territories surrounding this one, for now
		/* final Map<Territory, Collection<Unit>> attackingFromMap = new HashMap<Territory, Collection<Unit>>();
		attackingFromMap.put(m_location, m_attackingUnits);
		for (final Territory t : m_data.getMap().getNeighbors(m_location))
		{
			attackingFromMap.put(t, m_attackingUnits);
		}*/
		final AggregateResults rVal = new AggregateResults(count);
		final BattleTracker battleTracker = new BattleTracker();
		BattleCalculator.EnableCasualtySortingCaching();
		for (int i = 0; i < count && !m_cancelled; i++)
		{
			final CompositeChange allChanges = new CompositeChange();
			final DummyDelegateBridge bridge1 = new DummyDelegateBridge(m_attacker, m_data, allChanges, m_keepOneAttackingLandUnit, m_retreatAfterRound);
			final GameDelegateBridge bridge = new GameDelegateBridge(bridge1);
			final MustFightBattle battle = new MustFightBattle(m_location, m_attacker, m_data, battleTracker);
			bridge1.setBattle(battle);
			battle.setHeadless(true);
			battle.isAmphibious();
			battle.setUnits(m_defendingUnits, m_attackingUnits, m_bombardingUnits, (m_amphibious ? m_attackingUnits : new ArrayList<Unit>()), m_defender, m_territoryEffects);
			// battle.setAttackingFromAndMap(attackingFromMap);
			battle.fight(bridge);
			rVal.addResult(new BattleResults(battle, m_data));
			// restore the game to its original state
			new ChangePerformer(m_data).perform(allChanges.invert());
			battleTracker.clear();
			battleTracker.clearBattleRecords();
		}
		BattleCalculator.DisableCasualtySortingCaching();
		rVal.setTime(System.currentTimeMillis() - start);
		return rVal;
	}
	
	public void cancel()
	{
		m_cancelled = true;
	}
}


class DummyDelegateBridge implements IDelegateBridge
{
	private final PlainRandomSource m_randomSource = new PlainRandomSource();
	private final DummyDisplay m_display = new DummyDisplay();
	private final DummyPlayer m_attackingPlayer;
	private final DummyPlayer m_defendingPlayer;
	private final PlayerID m_attacker;
	private final DelegateHistoryWriter m_writer = new DelegateHistoryWriter(new DummyGameModifiedChannel());
	private final CompositeChange m_allChanges;
	private final GameData m_data;
	private final ChangePerformer m_changePerformer;
	private MustFightBattle m_battle = null;
	
	public DummyDelegateBridge(final PlayerID attacker, final GameData data, final CompositeChange allChanges, final boolean attackerKeepOneLandUnit, final int retreatAfterRound)
	{
		m_attackingPlayer = new DummyPlayer(this, true, "battle calc dummy", "None (AI)", attackerKeepOneLandUnit, retreatAfterRound);
		m_defendingPlayer = new DummyPlayer(this, false, "battle calc dummy", "None (AI)", false, retreatAfterRound);
		m_data = data;
		m_attacker = attacker;
		m_allChanges = allChanges;
		m_changePerformer = new ChangePerformer(m_data);
	}
	
	public GameData getData()
	{
		return m_data;
	}
	
	public void leaveDelegateExecution()
	{
	}
	
	public Properties getStepProperties()
	{
		throw new UnsupportedOperationException();
	}
	
	public String getStepName()
	{
		throw new UnsupportedOperationException();
	}
	
	public IRemotePlayer getRemotePlayer(final PlayerID id)
	{
		if (id.equals(m_attacker))
			return m_attackingPlayer;
		else
			return m_defendingPlayer;
	}
	
	public IRemotePlayer getRemotePlayer()
	{
		// the current player is attacker
		return m_attackingPlayer;
	}
	
	public int[] getRandom(final int max, final int count, final PlayerID player, final DiceType diceType, final String annotation)
	{
		return m_randomSource.getRandom(max, count, annotation);
	}
	
	public int getRandom(final int max, final PlayerID player, final DiceType diceType, final String annotation)
	{
		return m_randomSource.getRandom(max, annotation);
	}
	
	public PlayerID getPlayerID()
	{
		return m_attacker;
	}
	
	public IDelegateHistoryWriter getHistoryWriter()
	{
		return m_writer;
	}
	
	public IDisplay getDisplayChannelBroadcaster()
	{
		return m_display;
	}
	
	public void enterDelegateExecution()
	{
	}
	
	public void addChange(final Change aChange)
	{
		if (!(aChange instanceof UnitHitsChange))
			return;
		m_allChanges.add(aChange);
		m_changePerformer.perform(aChange);
	}
	
	public void stopGameSequence()
	{
	}
	
	public MustFightBattle getBattle()
	{
		return m_battle;
	}
	
	public void setBattle(final MustFightBattle battle)
	{
		m_battle = battle;
	}
};


class DummyGameModifiedChannel implements IGameModifiedChannel
{
	public void addChildToEvent(final String text, final Object renderingData)
	{
	}
	
	public void gameDataChanged(final Change aChange)
	{
	}
	
	/*public void setRenderingData(final Object renderingData)
	{
	}*/

	public void shutDown()
	{
	}
	
	public void startHistoryEvent(final String event)
	{
	}
	
	public void stepChanged(final String stepName, final String delegateName, final PlayerID player, final int round, final String displayName, final boolean loadedFromSavedGame)
	{
	}
	
	public void startHistoryEvent(final String event, final Object renderingData)
	{
	}
}


class DummyPlayer extends AbstractAI
{
	private final boolean m_keepAtLeastOneLand;
	private final int m_retreatAfterRound;
	private final DummyDelegateBridge m_bridge;
	private final boolean m_isAttacker;
	
	public DummyPlayer(final DummyDelegateBridge dummyDelegateBridge, final boolean attacker, final String name, final String type, final boolean keepAtLeastOneLand, final int retreatAfterRound)
	{
		super(name, type);
		m_keepAtLeastOneLand = keepAtLeastOneLand;
		m_retreatAfterRound = retreatAfterRound;
		m_bridge = dummyDelegateBridge;
		m_isAttacker = attacker;
	}
	
	private MustFightBattle getBattle()
	{
		return m_bridge.getBattle();
	}
	
	private Collection<Unit> getOurUnits()
	{
		final MustFightBattle battle = getBattle();
		if (battle == null)
			return null;
		return (m_isAttacker ? battle.getAttackingUnits() : battle.getDefendingUnits());
	}
	
	private Collection<Unit> getEnemyUnits()
	{
		final MustFightBattle battle = getBattle();
		if (battle == null)
			return null;
		return (m_isAttacker ? battle.getDefendingUnits() : battle.getAttackingUnits());
	}
	
	@Override
	protected void move(final boolean nonCombat, final IMoveDelegate moveDel, final GameData data, final PlayerID player)
	{
	}
	
	@Override
	protected void place(final boolean placeForBid, final IAbstractPlaceDelegate placeDelegate, final GameData data, final PlayerID player)
	{
	}
	
	@Override
	protected void purchase(final boolean purcahseForBid, final int PUsToSpend, final IPurchaseDelegate purchaseDelegate, final GameData data, final PlayerID player)
	{
	}
	
	@Override
	protected void tech(final ITechDelegate techDelegate, final GameData data, final PlayerID player)
	{
	}
	
	public boolean confirmMoveInFaceOfAA(final Collection<Territory> aaFiringTerritories)
	{
		throw new UnsupportedOperationException();
	}
	
	public Collection<Unit> getNumberOfFightersToMoveToNewCarrier(final Collection<Unit> fightersThatCanBeMoved, final Territory from)
	{
		throw new UnsupportedOperationException();
	}
	
	/**
	 * The battle calc doesn't actually care if you have available territories to retreat to or not.
	 * It will always let you retreat to the 'current' territory (the battle territory), even if that is illegal.
	 * This is because the battle calc does not know where the attackers are actually coming from.
	 */
	public Territory retreatQuery(final GUID battleID, final boolean submerge, final Territory battleSite, final Collection<Territory> possibleTerritories, final String message)
	{
		// null = do not retreat
		if (possibleTerritories.isEmpty())
			return null;
		if (submerge)
		{
			// submerge if all air vs subs
			final CompositeMatch<Unit> seaSub = new CompositeMatchAnd<Unit>(Matches.UnitIsSea, Matches.UnitIsSub);
			final CompositeMatch<Unit> planeNotDestroyer = new CompositeMatchAnd<Unit>(Matches.UnitIsAir, Matches.UnitIsDestroyer.invert());
			final Collection<Unit> ourUnits = getOurUnits();
			final Collection<Unit> enemyUnits = getEnemyUnits();
			if (ourUnits == null || enemyUnits == null)
				return null;
			if (enemyUnits.size() > 0 && Match.allMatch(ourUnits, seaSub) && Match.allMatch(enemyUnits, planeNotDestroyer))
				return possibleTerritories.iterator().next();
			return null;
		}
		else
		{
			final MustFightBattle battle = getBattle();
			if (battle == null)
				return null;
			if (battle.getBattleRound() >= m_retreatAfterRound)
				return possibleTerritories.iterator().next();
			return null;
		}
	}
	
	/*public Collection<Unit> scrambleQuery(final GUID battleID, final Collection<Territory> possibleTerritories, final String message, final PlayerID player)
	{
		// no scramble
		return null;
	}*/

	public HashMap<Territory, Collection<Unit>> scrambleUnitsQuery(final Territory scrambleTo, final Map<Territory, Tuple<Collection<Unit>, Collection<Unit>>> possibleScramblers)
	{
		return null;
	}
	
	public Collection<Unit> selectUnitsQuery(final Territory current, final Collection<Unit> possible, final String message)
	{
		return null;
	}
	
	// Added new collection autoKilled to handle killing units prior to casualty selection
	public CasualtyDetails selectCasualties(final Collection<Unit> selectFrom, final Map<Unit, Collection<Unit>> dependents, final int count, final String message, final DiceRoll dice,
				final PlayerID hit, final CasualtyList defaultCasualties, final GUID battleID)
	{
		final List<Unit> rDamaged = new ArrayList<Unit>();
		final List<Unit> rKilled = new ArrayList<Unit>();
		rDamaged.addAll(defaultCasualties.getDamaged());
		rKilled.addAll(defaultCasualties.getKilled());
		/*for(Unit unit : defaultCasualties)
		{
		    boolean twoHit = UnitAttachment.get(unit.getType()).isTwoHit();
		    //if it appears twice it then it both damaged and killed
		    if(unit.getHits() == 0 && twoHit && !rDamaged.contains(unit))
		        rDamaged.add(unit);
		    else
		        rKilled.add(unit);
		}*/
		if (m_keepAtLeastOneLand)
		{
			final List<Unit> notKilled = new ArrayList<Unit>(selectFrom);
			notKilled.removeAll(rKilled);
			// no land units left, but we
			// have a non land unit to kill
			// and land unit was killed
			if (!Match.someMatch(notKilled, Matches.UnitIsLand) && Match.someMatch(notKilled, Matches.UnitIsNotLand) && Match.someMatch(rKilled, Matches.UnitIsLand))
			{
				final List<Unit> notKilledAndNotLand = Match.getMatches(notKilled, Matches.UnitIsNotLand);
				// sort according to cost
				Collections.sort(notKilledAndNotLand, AIUtils.getCostComparator());
				// remove the last killed unit, this should be the strongest
				rKilled.remove(rKilled.size() - 1);
				// add the cheapest unit
				rKilled.add(notKilledAndNotLand.get(0));
			}
		}
		final CasualtyDetails m2 = new CasualtyDetails(rKilled, rDamaged, false);
		return m2;
	}
	
	public Territory selectTerritoryForAirToLand(final Collection<Territory> candidates, final Territory currentTerritory, final String unitMessage)
	{
		throw new UnsupportedOperationException();
	}
	
	public boolean shouldBomberBomb(final Territory territory)
	{
		throw new UnsupportedOperationException();
	}
	
	public Unit whatShouldBomberBomb(final Territory territory, final Collection<Unit> potentialTargets, final Collection<Unit> bombers)
	{
		throw new UnsupportedOperationException();
	}
	
	/* (non-Javadoc)
	 * @see games.strategy.triplea.player.ITripleaPlayer#selectFixedDice(int, java.lang.String)
	 */
	public int[] selectFixedDice(final int numRolls, final int hitAt, final boolean hitOnlyIfEquals, final String message, final int diceSides)
	{
		final int[] dice = new int[numRolls];
		for (int i = 0; i < numRolls; i++)
		{
			dice[i] = (int) Math.ceil(Math.random() * diceSides);
		}
		return dice;
	}
}
