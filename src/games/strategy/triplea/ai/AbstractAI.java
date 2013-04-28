package games.strategy.triplea.ai;

import games.strategy.common.player.AbstractBaseAI;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.gamePlayer.IGamePlayer;
import games.strategy.net.GUID;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attatchments.PlayerAttachment;
import games.strategy.triplea.attatchments.PoliticalActionAttachment;
import games.strategy.triplea.attatchments.UnitAttachment;
import games.strategy.triplea.delegate.DelegateFinder;
import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.delegate.IBattle.BattleType;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.PoliticsDelegate;
import games.strategy.triplea.delegate.dataObjects.BattleListing;
import games.strategy.triplea.delegate.dataObjects.CasualtyDetails;
import games.strategy.triplea.delegate.dataObjects.CasualtyList;
import games.strategy.triplea.delegate.remote.IAbstractForumPosterDelegate;
import games.strategy.triplea.delegate.remote.IAbstractPlaceDelegate;
import games.strategy.triplea.delegate.remote.IBattleDelegate;
import games.strategy.triplea.delegate.remote.IMoveDelegate;
import games.strategy.triplea.delegate.remote.IPoliticsDelegate;
import games.strategy.triplea.delegate.remote.IPurchaseDelegate;
import games.strategy.triplea.delegate.remote.ITechDelegate;
import games.strategy.triplea.player.ITripleaPlayer;
import games.strategy.util.IntegerMap;
import games.strategy.util.Match;
import games.strategy.util.Tuple;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

/**
 * Base class for ais.
 * <p>
 * 
 * Control pausing with the AI pause menu option
 * 
 * AI's should note that any data that is stored in the ai instance, will be lost when the game is restarted. We cannot save data with an AI, since the player may choose to restart the game with a different ai, or with a human player.
 * <p>
 * 
 * If an ai finds itself starting in the middle of a move phase, or the middle of a purchase phase, (as would happen if a player saved the game during the middle of an AI's move phase) it is acceptable for the ai to play badly for a turn, but the ai should recover, and play correctly when the next phase of the game starts.
 * <p>
 * 
 * As a rule, nothing that changes GameData should be in here (it should be in a delegate, and done through an IDelegate using a change).
 * <p>
 * 
 * @author sgb
 */
public abstract class AbstractAI extends AbstractBaseAI implements ITripleaPlayer, IGamePlayer
{
	private final static Logger s_logger = Logger.getLogger(AbstractAI.class.getName());
	
	/**
	 * 
	 * @param name
	 *            - the name of the player (the nation)
	 * @param type
	 *            - the type of player we are
	 */
	public AbstractAI(final String name, final String type)
	{
		super(name, type);
	}
	
	public final Class<ITripleaPlayer> getRemotePlayerType()
	{
		return ITripleaPlayer.class;
	}
	
	/************************
	 * The following methods are called when the AI starts a phase.
	 *************************/
	/**
	 * It is the AI's turn to purchase units.
	 * 
	 * @param purcahseForBid
	 *            - is this a bid purchase, or a normal purchase
	 * @param PUsToSpend
	 *            - how many PUs we have to spend
	 * @param purchaseDelegate
	 *            - the purchase delgate to buy things with
	 * @param data
	 *            - the GameData
	 * @param player
	 *            - the player to buy for
	 */
	protected abstract void purchase(boolean purchaseForBid, int PUsToSpend, IPurchaseDelegate purchaseDelegate, GameData data, PlayerID player);
	
	/**
	 * It is the AI's turn to roll for technology.
	 * 
	 * @param techDelegate
	 *            - the tech delegate to roll for
	 * @param data
	 *            - the game data
	 * @param player
	 *            - the player to roll tech for
	 */
	protected abstract void tech(ITechDelegate techDelegate, GameData data, PlayerID player);
	
	/**
	 * It is the AI's turn to move. Make all moves before returning from this method.
	 * 
	 * @param nonCombat
	 *            - are we moving in combat, or non combat
	 * @param moveDel
	 *            - the move delegate to make moves with
	 * @param data
	 *            - the current game data
	 * @param player
	 *            - the player to move with
	 */
	protected abstract void move(boolean nonCombat, IMoveDelegate moveDel, GameData data, PlayerID player);
	
	/**
	 * It is the AI's turn to place units. get the units available to place with player.getUnits()
	 * 
	 * @param placeForBid
	 *            - is this a placement for bid
	 * @param placeDelegate
	 *            - the place delegate to place with
	 * @param data
	 *            - the current Game Data
	 * @param player
	 *            - the player to place for
	 */
	protected abstract void place(boolean placeForBid, IAbstractPlaceDelegate placeDelegate, GameData data, PlayerID player);
	
	/******************************************
	 * The following methods the AI may choose to implemenmt,
	 * but in general won't
	 * 
	 *******************************************/
	public Territory selectBombardingTerritory(final Unit unit, final Territory unitTerritory, final Collection<Territory> territories, final boolean noneAvailable)
	{
		// return the first one
		return territories.iterator().next();
	}
	
	public boolean selectAttackSubs(final Territory unitTerritory)
	{
		return true;
	}
	
	public boolean selectAttackTransports(final Territory unitTerritory)
	{
		return true;
	}
	
	public boolean selectAttackUnits(final Territory unitTerritory)
	{
		return true;
	}
	
	public boolean selectShoreBombard(final Territory unitTerritory)
	{
		return true;
	}
	
	public boolean confirmMoveKamikaze()
	{
		return false;
	}
	
	public boolean confirmMoveHariKari()
	{
		return false;
	}
	
	public Territory whereShouldRocketsAttack(final Collection<Territory> candidates, final Territory from)
	{
		// just use the first one
		return candidates.iterator().next();
	}
	
	/*
	 * 
	 * 
	 * @see games.strategy.triplea.player.ITripleaPlayer#selectCasualties(java.lang.String,
	 *      java.util.Collection, java.util.Map, int, java.lang.String,
	 *      games.strategy.triplea.delegate.DiceRoll,
	 *      games.strategy.engine.data.PlayerID, java.util.List)
	 *
	 *      Added new collection autoKilled to handle killing units prior to casualty selection
	 */
	public CasualtyDetails selectCasualties(final Collection<Unit> selectFrom, final Map<Unit, Collection<Unit>> dependents, final int count, final String message, final DiceRoll dice,
				final PlayerID hit, final CasualtyList defaultCasualties, final GUID battleID, final Territory battlesite, final boolean allowMultipleHitsPerUnit)
	{
		return new CasualtyDetails(defaultCasualties, true);
	}
	
	/*
	 * @see games.strategy.triplea.player.ITripleaPlayer#shouldBomberBomb(games.strategy.engine.data.Territory)
	 */
	public Unit whatShouldBomberBomb(final Territory territory, final Collection<Unit> potentialTargets, final Collection<Unit> bombers)
	{
		if (potentialTargets == null || potentialTargets.isEmpty())
			return null;
		final Collection<Unit> factories = Match.getMatches(potentialTargets, Matches.UnitCanProduceUnitsAndCanBeDamaged);
		if (factories.isEmpty())
			return potentialTargets.iterator().next();
		return factories.iterator().next();
	}
	
	/*
	 * @see games.strategy.triplea.player.ITripleaPlayer#getNumberOfFightersToMoveToNewCarrier(java.util.Collection, games.strategy.engine.data.Territory)
	 */
	public Collection<Unit> getNumberOfFightersToMoveToNewCarrier(final Collection<Unit> fightersThatCanBeMoved, final Territory from)
	{
		final List<Unit> rVal = new ArrayList<Unit>();
		for (final Unit fighter : fightersThatCanBeMoved)
		{
			if (Math.random() < 0.8)
				rVal.add(fighter);
		}
		return rVal;
	}
	
	/*
	 * @see games.strategy.triplea.player.ITripleaPlayer#selectTerritoryForAirToLand(java.util.Collection, java.lang.String)
	 */
	public Territory selectTerritoryForAirToLand(final Collection<Territory> candidates, final Territory currentTerritory, final String unitMessage)
	{
		return candidates.iterator().next();
	}
	
	public boolean confirmMoveInFaceOfAA(final Collection<Territory> aaFiringTerritories)
	{
		return true;
	}
	
	public Territory retreatQuery(final GUID battleID, final boolean submerge, final Territory battleTerritory, final Collection<Territory> possibleTerritories, final String message)
	{
		return null;
	}
	
	public HashMap<Territory, Collection<Unit>> scrambleUnitsQuery(final Territory scrambleTo, final Map<Territory, Tuple<Collection<Unit>, Collection<Unit>>> possibleScramblers)
	{
		return null;
	}
	
	public Collection<Unit> selectUnitsQuery(final Territory current, final Collection<Unit> possible, final String message)
	{
		return null;
	}
	
	public abstract boolean shouldBomberBomb(final Territory territory);
	
	public boolean acceptPoliticalAction(final String acceptanceQuestion)
	{
		// we are dead, just accept
		if (!getPlayerID().amNotDeadYet(getGameData()))
			return true;
		if (Math.random() < .5)
			return true;
		return false;
	}
	
	public HashMap<Territory, HashMap<Unit, IntegerMap<Resource>>> selectKamikazeSuicideAttacks(final HashMap<Territory, Collection<Unit>> possibleUnitsToAttack)
	{
		final PlayerID id = getPlayerID();
		// we are going to just assign random attacks to each unit randomly, til we run out of tokens to attack with.
		final PlayerAttachment pa = PlayerAttachment.get(id);
		if (pa == null)
			return null;
		final IntegerMap<Resource> resourcesAndAttackValues = pa.getSuicideAttackResources();
		if (resourcesAndAttackValues.size() <= 0)
			return null;
		final IntegerMap<Resource> playerResourceCollection = id.getResources().getResourcesCopy();
		final IntegerMap<Resource> attackTokens = new IntegerMap<Resource>();
		for (final Resource possible : resourcesAndAttackValues.keySet())
		{
			final int amount = playerResourceCollection.getInt(possible);
			if (amount > 0)
				attackTokens.put(possible, amount);
		}
		if (attackTokens.size() <= 0)
			return null;
		final HashMap<Territory, HashMap<Unit, IntegerMap<Resource>>> rVal = new HashMap<Territory, HashMap<Unit, IntegerMap<Resource>>>();
		for (final Entry<Territory, Collection<Unit>> entry : possibleUnitsToAttack.entrySet())
		{
			if (attackTokens.size() <= 0)
				continue;
			final Territory t = entry.getKey();
			final List<Unit> targets = new ArrayList<Unit>(entry.getValue());
			Collections.shuffle(targets);
			for (final Unit u : targets)
			{
				if (attackTokens.size() <= 0)
					continue;
				final IntegerMap<Resource> rMap = new IntegerMap<Resource>();
				final Resource r = attackTokens.keySet().iterator().next();
				final int num = Math.min(attackTokens.getInt(r), ((UnitAttachment.get(u.getType()).getIsTwoHit() ? 2 : 1)
							* (Math.random() < .3 ? 1 : (Math.random() < .5 ? 2 : 3))));
				rMap.put(r, num);
				HashMap<Unit, IntegerMap<Resource>> attMap = rVal.get(t);
				if (attMap == null)
					attMap = new HashMap<Unit, IntegerMap<Resource>>();
				attMap.put(u, rMap);
				rVal.put(t, attMap);
				attackTokens.add(r, -num);
				if (attackTokens.getInt(r) <= 0)
					attackTokens.removeKey(r);
			}
		}
		return rVal;
	}
	
	/*****************************************
	 * The following methods are more for the ui, and the
	 * ai will generally not care
	 * 
	 *****************************************/
	public void battleInfoMessage(final String shortMessage, final DiceRoll dice)
	{
	}
	
	public void confirmEnemyCasualties(final GUID battleId, final String message, final PlayerID hitPlayer)
	{
	}
	
	public void retreatNotificationMessage(final Collection<Unit> units)
	{
	}
	
	public void reportError(final String error)
	{
	}
	
	public void reportMessage(final String message, final String title)
	{
	}
	
	public void confirmOwnCasualties(final GUID battleId, final String message)
	{
		pause();
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
	
	/*****************************************
	 * Game Player Methods
	 * 
	 *****************************************/
	/**
	 * The given phase has started. We parse the phase name and call the apropiate method.
	 */
	@Override
	public final void start(final String name)
	{
		final PlayerID id = getPlayerID();
		if (name.endsWith("Bid"))
		{
			final IPurchaseDelegate purchaseDelegate = (IPurchaseDelegate) getPlayerBridge().getRemote();
			/*if (!purchaseDelegate.stuffToDoInThisDelegate())
				return;*/
			final String propertyName = id.getName() + " bid";
			final int bidAmount = getGameData().getProperties().get(propertyName, 0);
			purchase(true, bidAmount, purchaseDelegate, getGameData(), id);
		}
		else if (name.endsWith("Purchase"))
		{
			final IPurchaseDelegate purchaseDelegate = (IPurchaseDelegate) getPlayerBridge().getRemote();
			/*if (!purchaseDelegate.stuffToDoInThisDelegate())
				return;*/
			final Resource PUs = getGameData().getResourceList().getResource(Constants.PUS);
			final int leftToSpend = id.getResources().getQuantity(PUs);
			purchase(false, leftToSpend, purchaseDelegate, getGameData(), id);
		}
		else if (name.endsWith("Tech"))
		{
			final ITechDelegate techDelegate = (ITechDelegate) getPlayerBridge().getRemote();
			/*if (!techDelegate.stuffToDoInThisDelegate())
				return;*/
			tech(techDelegate, getGameData(), id);
		}
		else if (name.endsWith("Move"))
		{
			
			final IMoveDelegate moveDel = (IMoveDelegate) getPlayerBridge().getRemote();
			/* if (!moveDel.stuffToDoInThisDelegate())
				return; */
			if (name.endsWith("AirborneCombatMove"))
			{
				return;
				// if (!SpecialMoveDelegate.allowAirborne(id, getGameData()))
				// return;
				// airborneMove(); // TODO: implement me
			}
			else
			{
				move(name.endsWith("NonCombatMove"), moveDel, getGameData(), id);
			}
		}
		else if (name.endsWith("Battle"))
			battle((IBattleDelegate) getPlayerBridge().getRemote(), getGameData(), id);
		else if (name.endsWith("Politics"))
			politicalActions();
		else if (name.endsWith("Place"))
		{
			final IAbstractPlaceDelegate placeDel = (IAbstractPlaceDelegate) getPlayerBridge().getRemote();
			/*if (!placeDel.stuffToDoInThisDelegate())
				return;*/
			place(name.indexOf("Bid") != -1, placeDel, getGameData(), id);
		}
		else if (name.endsWith("EndTurn"))
		{
			endTurn((IAbstractForumPosterDelegate) getPlayerBridge().getRemote(), getGameData(), id);
		}
	}
	
	/**
	 * No need to override this.
	 */
	protected void endTurn(final IAbstractForumPosterDelegate endTurnForumPosterDelegate, final GameData data, final PlayerID player)
	{
		// we should not override this...
	}
	
	/**
	 * It is the AI's turn to fight. Subclasses may override this if they want, but
	 * generally the AI does not need to worry about the order of fighting battles.
	 * 
	 * @param battleDelegate
	 *            the battle delegate to query for battles not fought and the
	 * @param data
	 *            - the current GameData
	 * @param player
	 *            - the player to fight for
	 */
	protected void battle(final IBattleDelegate battleDelegate, final GameData data, final PlayerID player)
	{
		// generally all AI's will follow the same logic.
		// loop until all battles are fought.
		// rather than try to analyze battles to figure out which must be fought before others
		// as in the case of a naval battle preceding an amphibious attack,
		// keep trying to fight every battle
		while (true)
		{
			final BattleListing listing = battleDelegate.getBattles();
			// all fought
			if (listing.isEmpty())
				return;
			for (final Entry<BattleType, Collection<Territory>> entry : listing.getBattles().entrySet())
			{
				for (final Territory current : entry.getValue())
				{
					final String error = battleDelegate.fightBattle(current, entry.getKey().isBombingRun(), entry.getKey());
					if (error != null)
						s_logger.fine(error);
				}
			}
		}
	}
	
	public void politicalActions()
	{
		final IPoliticsDelegate iPoliticsDelegate = (IPoliticsDelegate) getPlayerBridge().getRemote();
		/*
		if (!iPoliticsDelegate.stuffToDoInThisDelegate())
			return;
		*/
		final GameData data = getGameData();
		final PlayerID id = getPlayerID();
		final float numPlayers = data.getPlayerList().getPlayers().size();
		final PoliticsDelegate politicsDelegate = DelegateFinder.politicsDelegate(data);
		// final HashMap<ICondition, Boolean> testedConditions = DelegateFinder.politicsDelegate(data).getTestedConditions();//this is commented out because we want to test the conditions each time to make sure they are still valid
		if (Math.random() < .5)
		{
			final List<PoliticalActionAttachment> actionChoicesTowardsWar = BasicPoliticalAI.getPoliticalActionsTowardsWar(id, politicsDelegate.getTestedConditions(), data);
			if (actionChoicesTowardsWar != null && !actionChoicesTowardsWar.isEmpty())
			{
				Collections.shuffle(actionChoicesTowardsWar);
				int i = 0;
				final double random = Math.random(); // should we use bridge's random source here?
				int MAX_WAR_ACTIONS_PER_TURN = (random < .5 ? 0 : (random < .9 ? 1 : (random < .99 ? 2 : (int) numPlayers / 2)));
				if ((MAX_WAR_ACTIONS_PER_TURN > 0) && (Match.countMatches(data.getRelationshipTracker().getRelationships(id), Matches.RelationshipIsAtWar)) / numPlayers < 0.4)
				{
					if (Math.random() < .9)
						MAX_WAR_ACTIONS_PER_TURN = 0;
					else
						MAX_WAR_ACTIONS_PER_TURN = 1;
				}
				final Iterator<PoliticalActionAttachment> actionWarIter = actionChoicesTowardsWar.iterator();
				while (actionWarIter.hasNext() && MAX_WAR_ACTIONS_PER_TURN > 0)
				{
					final PoliticalActionAttachment action = actionWarIter.next();
					if (!Matches.PoliticalActionCanBeAttempted(politicsDelegate.getTestedConditions()).match(action))
						continue;
					i++;
					if (i > MAX_WAR_ACTIONS_PER_TURN)
						break;
					iPoliticsDelegate.attemptAction(action);
				}
			}
		}
		else
		{
			final List<PoliticalActionAttachment> actionChoicesOther = BasicPoliticalAI.getPoliticalActionsOther(id, politicsDelegate.getTestedConditions(), data);
			if (actionChoicesOther != null && !actionChoicesOther.isEmpty())
			{
				Collections.shuffle(actionChoicesOther);
				int i = 0;
				final double random = Math.random(); // should we use bridge's random source here?
				final int MAX_OTHER_ACTIONS_PER_TURN = (random < .3 ? 0 : (random < .6 ? 1 : (random < .9 ? 2 : (random < .99 ? 3 : (int) numPlayers))));
				final Iterator<PoliticalActionAttachment> actionOtherIter = actionChoicesOther.iterator();
				while (actionOtherIter.hasNext() && MAX_OTHER_ACTIONS_PER_TURN > 0)
				{
					final PoliticalActionAttachment action = actionOtherIter.next();
					if (!Matches.PoliticalActionCanBeAttempted(politicsDelegate.getTestedConditions()).match(action))
						continue;
					if (action.getCostPU() > 0 && action.getCostPU() > id.getResources().getQuantity(Constants.PUS))
						continue;
					i++;
					if (i > MAX_OTHER_ACTIONS_PER_TURN)
						break;
					iPoliticsDelegate.attemptAction(action);
				}
			}
		}
	}
}
