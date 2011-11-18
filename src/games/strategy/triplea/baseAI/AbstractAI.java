package games.strategy.triplea.baseAI;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.gamePlayer.IGamePlayer;
import games.strategy.engine.gamePlayer.IPlayerBridge;
import games.strategy.net.GUID;
import games.strategy.triplea.Constants;
import games.strategy.triplea.Properties;
import games.strategy.triplea.attatchments.PoliticalActionAttachment;
import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.dataObjects.BattleListing;
import games.strategy.triplea.delegate.remote.IAbstractPlaceDelegate;
import games.strategy.triplea.delegate.remote.IBattleDelegate;
import games.strategy.triplea.delegate.remote.IMoveDelegate;
import games.strategy.triplea.delegate.remote.IPoliticsDelegate;
import games.strategy.triplea.delegate.remote.IPurchaseDelegate;
import games.strategy.triplea.delegate.remote.ITechDelegate;
import games.strategy.triplea.player.ITripleaPlayer;
import games.strategy.triplea.ui.UIContext;
import games.strategy.util.Match;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
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
public abstract class AbstractAI implements ITripleaPlayer, IGamePlayer
{
	private final static Logger s_logger = Logger.getLogger(AbstractAI.class.getName());
	
	/**
	 * Pause the game to allow the human player to see what is going on.
	 * 
	 */
	protected void pause()
	{
		
		try
		{
			Thread.sleep(UIContext.getAIPauseDuration());
		} catch (InterruptedException e)
		{
			e.printStackTrace();
		} catch (Exception ex)
		{
		}
		
	}
	
	private final String m_name;
	private final String m_type;
	private IPlayerBridge m_bridge;
	private PlayerID m_id;
	
	/**
	 * 
	 * @param name - the name of the player (the nation)
	 * @param type - the type of player we are
	 */
	public AbstractAI(String name, String type)
	{
		m_name = name;
		m_type = type;
	}
	
	public final void initialize(IPlayerBridge bridge, PlayerID id)
	{
		m_bridge = bridge;
		m_id = id;
	}
	
	/************************
	 * Allow the AI to get game data, playerID
	 *************************/
	
	/**
	 * Get the GameData for the game.
	 */
	protected final GameData getGameData()
	{
		return m_bridge.getGameData();
	}
	
	/**
	 * Get the IPlayerBridge for this game player.
	 */
	protected final IPlayerBridge getPlayerBridge()
	{
		return m_bridge;
	}
	
	/**
	 * What player are we playing.
	 */
	protected final PlayerID getWhoAmI()
	{
		return m_id;
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
	protected abstract void purchase(boolean purcahseForBid, int PUsToSpend, IPurchaseDelegate purchaseDelegate, GameData data, PlayerID player);
	
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
	protected void battle(IBattleDelegate battleDelegate, GameData data, PlayerID player)
	{
		// generally all AI's will follow the same logic.
		
		// loop until all battles are fought.
		// rather than try to analyze battles to figure out which must be fought before others
		// as in the case of a naval battle preceding an amphibious attack,
		// keep trying to fight every battle
		while (true)
		{
			
			BattleListing listing = battleDelegate.getBattles();
			
			// all fought
			if (listing.getBattles().isEmpty() && listing.getStrategicRaids().isEmpty())
				return;
			
			Iterator<Territory> raidBattles = listing.getStrategicRaids().iterator();
			
			// fight strategic bombing raids
			while (raidBattles.hasNext())
			{
				Territory current = raidBattles.next();
				String error = battleDelegate.fightBattle(current, true);
				if (error != null)
					s_logger.fine(error);
			}
			
			Iterator<Territory> nonRaidBattles = listing.getBattles().iterator();
			
			// fight normal battles
			while (nonRaidBattles.hasNext())
			{
				Territory current = nonRaidBattles.next();
				String error = battleDelegate.fightBattle(current, false);
				if (error != null)
					s_logger.fine(error);
			}
		}
	}
	
	/******************************************
	 * The following methods the AI may choose to implemenmt,
	 * but in general won't
	 * 
	 *******************************************/
	
	public Territory selectBombardingTerritory(Unit unit, Territory unitTerritory, Collection<Territory> territories, boolean noneAvailable)
	{
		// return the first one
		return territories.iterator().next();
	}
	
	public boolean selectAttackSubs(Territory unitTerritory)
	{
		return true;
	}
	
	public boolean selectAttackTransports(Territory unitTerritory)
	{
		return true;
	}
	
	public boolean selectAttackUnits(Territory unitTerritory)
	{
		return true;
	}
	
	public boolean selectShoreBombard(Territory unitTerritory)
	{
		return true;
	}
	
	public Territory whereShouldRocketsAttack(Collection<Territory> candidates, Territory from)
	{
		// just use the first one
		return candidates.iterator().next();
	}
	
	public boolean confirmMoveKamikaze()
	{
		return false;
	}
	
	public boolean confirmMoveHariKari()
	{
		return false;
	}
	
	public boolean acceptPoliticalAction(String acceptanceQuestion)
	{
		// we are dead, just accept
		if (!m_id.amNotDeadYet())
			return true;
		// should we use bridge's random source here?
		if (Math.random() < .5)
			return true;
		return false;
	}
	
	/*****************************************
	 * The following methods are more for the ui, and the
	 * ai will generally not care
	 * 
	 *****************************************/
	
	public void battleInfoMessage(String shortMessage, DiceRoll dice)
	{
	}
	
	public void reportPoliticalMessage(String message)
	{
	}
	
	public void confirmEnemyCasualties(GUID battleId, String message, PlayerID hitPlayer)
	{
	}
	
	public void retreatNotificationMessage(Collection<Unit> units)
	{
	}
	
	public void reportError(String error)
	{
	}
	
	public void reportMessage(String message, String title)
	{
	}
	
	public void confirmOwnCasualties(GUID battleId, String message)
	{
		pause();
	}
	
	/*****************************************
	 * Game Player Methods
	 * 
	 *****************************************/
	
	/**
	 * The given phase has started. We parse the phase name and call the apropiate method.
	 */
	public final void start(String name)
	{
		if (name.endsWith("Bid"))
		{
			String propertyName = m_id.getName() + " bid";
			int bidAmount = Integer.parseInt(m_bridge.getGameData().getProperties().get(propertyName).toString());
			
			purchase(true, bidAmount, (IPurchaseDelegate) m_bridge.getRemote(), m_bridge.getGameData(), m_id);
		}
		else if (name.endsWith("Tech"))
		{
			if (!Properties.getTechDevelopment(getGameData()))
			{
				return;
			}
			tech((ITechDelegate) m_bridge.getRemote(), m_bridge.getGameData(), m_id);
		}
		else if (name.endsWith("Purchase"))
		{
			
			Resource PUs = m_bridge.getGameData().getResourceList().getResource(Constants.PUS);
			int leftToSpend = m_id.getResources().getQuantity(PUs);
			
			purchase(false, leftToSpend, (IPurchaseDelegate) m_bridge.getRemote(), m_bridge.getGameData(), m_id);
		}
		else if (name.endsWith("Move"))
			move(name.endsWith("NonCombatMove"), (IMoveDelegate) m_bridge.getRemote(), m_bridge.getGameData(), m_id);
		else if (name.endsWith("Battle"))
			battle((IBattleDelegate) m_bridge.getRemote(), m_bridge.getGameData(), m_id);
		else if (name.endsWith("Politics"))
			getPoliticalActions();
		else if (name.endsWith("Place"))
			place(name.indexOf("Bid") != -1, (IAbstractPlaceDelegate) m_bridge.getRemote(), m_bridge.getGameData(), m_id);
		else if (name.endsWith("EndTurn"))
		{
		}
	}
	
	public void getPoliticalActions() {
		if (!m_id.amNotDeadYet())
			return;
		GameData data = m_bridge.getGameData();
		float numPlayers = data.getPlayerList().getPlayers().size();
		IPoliticsDelegate politicsDelegate = (IPoliticsDelegate) m_bridge.getRemote();
		
		if (Math.random() < .5)
		{
			List<PoliticalActionAttachment> actionChoicesTowardsWar = BasicPoliticalAI.getPoliticalActionsTowardsWar(m_id);
			if (actionChoicesTowardsWar != null && !actionChoicesTowardsWar.isEmpty())
			{
				Collections.shuffle(actionChoicesTowardsWar);
				
				int i = 0;
				double random = Math.random(); // should we use bridge's random source here?
				int MAX_WAR_ACTIONS_PER_TURN = (random < .5 ? 0 : (random < .9 ? 1 : (random < .99 ? 2 : (int) numPlayers/2)));
				if ((MAX_WAR_ACTIONS_PER_TURN > 0) && ((float) Match.countMatches(data.getRelationshipTracker().getRelationships(m_id), Matches.RelationshipIsAtWar)) / numPlayers < 0.4)
				{
					if (Math.random() < .9)
						MAX_WAR_ACTIONS_PER_TURN = 0;
					else
						MAX_WAR_ACTIONS_PER_TURN = 1;

				}
				
				Iterator<PoliticalActionAttachment> actionWarIter = actionChoicesTowardsWar.iterator();
				while (actionWarIter.hasNext() && MAX_WAR_ACTIONS_PER_TURN > 0)
				{
					PoliticalActionAttachment action = actionWarIter.next();
					if (!Matches.PoliticalActionCanBeAttempted.match(action))
						continue;
					i++;
					if (i>MAX_WAR_ACTIONS_PER_TURN)
						break;
					politicsDelegate.attemptAction(action);
				}
			}
		}
		else
		{
			List<PoliticalActionAttachment> actionChoicesOther = BasicPoliticalAI.getPoliticalActionsOther(m_id);
			if (actionChoicesOther != null && !actionChoicesOther.isEmpty())
			{
				Collections.shuffle(actionChoicesOther);
				
				int i = 0;
				double random = Math.random(); // should we use bridge's random source here?
				final int MAX_OTHER_ACTIONS_PER_TURN = (random < .3 ? 0 : (random < .6 ? 1 : (random < .9 ? 2 : (random < .99 ? 3 : (int) numPlayers))));
				
				Iterator<PoliticalActionAttachment> actionOtherIter = actionChoicesOther.iterator();
				while (actionOtherIter.hasNext() && MAX_OTHER_ACTIONS_PER_TURN > 0)
				{
					PoliticalActionAttachment action = actionOtherIter.next();
					if (!Matches.PoliticalActionCanBeAttempted.match(action))
						continue;
					if (action.getCostPU() > 0 && action.getCostPU() > m_id.getResources().getQuantity(Constants.PUS))
						continue;
					i++;
					if (i>MAX_OTHER_ACTIONS_PER_TURN)
						break;
					politicsDelegate.attemptAction(action);
				}
			}
		}
	}

	public final Class<ITripleaPlayer> getRemotePlayerType()
	{
		return ITripleaPlayer.class;
	}
	
	public final String getName()
	{
		return m_name;
	}
	
	public final String getType()
	{
		return m_type;
	}
	
	public final PlayerID getID()
	{
		return m_id;
	}
	
}
