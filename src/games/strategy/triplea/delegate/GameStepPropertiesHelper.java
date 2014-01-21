package games.strategy.triplea.delegate;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameStep;
import games.strategy.engine.data.PlayerID;

import java.util.HashSet;
import java.util.Set;

/**
 * A helper class for determining Game Step Properties.
 * These are things such as whether a move phase is combat move or noncombat move,
 * or whether we are going to post to a forum during this end turn phase.
 * 
 * @author veqryn
 * 
 */
public class GameStepPropertiesHelper
{
	/**
	 * Do we skip posting the game summary and save to a forum or email?
	 */
	public static boolean isSkipPosting(final GameData data)
	{
		return Boolean.parseBoolean(data.getSequence().getStep().getProperties().getProperty(GameStep.PROPERTY_skipPosting, "false"));
	}
	
	/**
	 * What players is this turn summary for? If more than 1 player, whose phases are touching or intermeshed, then we will summarize for all those phases.
	 * 
	 * @return colon separated list of player names. could be empty. can be null if not set.
	 */
	public static Set<PlayerID> getTurnSummaryPlayers(final GameData data)
	{
		final String allowedPlayers = data.getSequence().getStep().getProperties().getProperty(GameStep.PROPERTY_turnSummaryPlayers);// parse allowed players
		final Set<PlayerID> allowedIDs;
		if (allowedPlayers != null)
		{
			allowedIDs = new HashSet<PlayerID>();
			for (final String p : allowedPlayers.split(":"))
			{
				final PlayerID id = data.getPlayerList().getPlayerID(p);
				if (id == null)
					System.err.println("gamePlay sequence step: " + data.getSequence().getStep().getName() + " stepProperty: " + GameStep.PROPERTY_turnSummaryPlayers + " player: " + p
								+ " DOES NOT EXIST");
				else
					allowedIDs.add(id);
			}
		}
		else
			allowedIDs = null;
		return allowedIDs;
	}
	
	/**
	 * For various things related to movement validation.
	 */
	public static boolean isCombatMove(final GameData data)
	{
		final String prop = data.getSequence().getStep().getProperties().getProperty(GameStep.PROPERTY_combatMove);
		if (prop != null)
			return Boolean.parseBoolean(prop);
		if (isCombatDelegate(data))
			return true;
		else if (isNonCombatDelegate(data))
			return false;
		else
			throw new IllegalStateException("Cannot determine combat or not");
	}
	
	/**
	 * For various things related to movement validation.
	 */
	public static boolean isNonCombatMove(final GameData data)
	{
		final String prop = data.getSequence().getStep().getProperties().getProperty(GameStep.PROPERTY_nonCombatMove);
		if (prop != null)
			return Boolean.parseBoolean(prop);
		if (isNonCombatDelegate(data))
			return true;
		else if (isCombatDelegate(data))
			return false;
		else
			throw new IllegalStateException("Cannot determine combat or not");
	}
	
	/**
	 * Fire rockets after phase is over. Normally would occur after combat move for WW2v2 and WW2v3, and after noncombat move for WW2v1.
	 */
	public static boolean isFireRocketsAfter(final GameData data)
	{
		final String prop = data.getSequence().getStep().getProperties().getProperty(GameStep.PROPERTY_fireRocketsAfter);
		if (prop != null)
			return Boolean.parseBoolean(prop);
		if (games.strategy.triplea.Properties.getWW2V2(data) || games.strategy.triplea.Properties.getWW2V3(data))
		{
			if (isCombatDelegate(data))
				return true;
		}
		else if (isNonCombatDelegate(data))
		{
			return true;
		}
		return false;
	}
	
	/**
	 * Repairs damaged units. Normally would occur at either start of combat move or end of turn, depending.
	 */
	public static boolean isRepairUnits(final GameData data)
	{
		final boolean repairAtStartAndOnlyOwn = games.strategy.triplea.Properties.getBattleshipsRepairAtBeginningOfRound(data);
		final boolean repairAtEndAndAll = games.strategy.triplea.Properties.getBattleshipsRepairAtEndOfRound(data);
		// if both are off, we do no repairing, no matter what
		if (!repairAtStartAndOnlyOwn && !repairAtEndAndAll)
			return false;
		final String prop = data.getSequence().getStep().getProperties().getProperty(GameStep.PROPERTY_repairUnits);
		if (prop != null)
			return Boolean.parseBoolean(prop);
		if (isCombatDelegate(data) && repairAtStartAndOnlyOwn)
			return true;
		else if (data.getSequence().getStep().getName().endsWith("EndTurn") && repairAtEndAndAll)
			return true;
		else
			return false;
	}
	
	/**
	 * Resets then gives bonus movement. Normally would occur at the start of combat movement phase.
	 */
	public static boolean isGiveBonusMovement(final GameData data)
	{
		final String prop = data.getSequence().getStep().getProperties().getProperty(GameStep.PROPERTY_giveBonusMovement);
		if (prop != null)
			return Boolean.parseBoolean(prop);
		if (isCombatDelegate(data))
			return true;
		return false;
	}
	
	/**
	 * Kills all air that can not land. Normally would occur both at the end of noncombat movement and also at end of placement phase.
	 */
	public static boolean isRemoveAirThatCanNotLand(final GameData data)
	{
		final String prop = data.getSequence().getStep().getProperties().getProperty(GameStep.PROPERTY_removeAirThatCanNotLand);
		if (prop != null)
			return Boolean.parseBoolean(prop);
		if (data.getSequence().getStep().getDelegate() != null && NoAirCheckPlaceDelegate.class.equals(data.getSequence().getStep().getDelegate().getClass()))
			return false;
		if (isNonCombatDelegate(data))
			return true;
		else if (data.getSequence().getStep().getName().endsWith("Place"))
			return true;
		return false;
	}
	
	/**
	 * For situations where player phases are intermeshed.
	 * Effects so far:
	 * Lets air live if the other players could put a carrier under it.
	 * 
	 * @return a set of player ids. if argument player is not null this set will definitely include that player, but if not the set could be empty. never null.
	 */
	public static Set<PlayerID> getCombinedTurns(final GameData data, final PlayerID player)
	{
		final String allowedPlayers = data.getSequence().getStep().getProperties().getProperty(GameStep.PROPERTY_combinedTurns);// parse allowed players
		final Set<PlayerID> allowedIDs = new HashSet<PlayerID>();
		if (player != null)
			allowedIDs.add(player);
		if (allowedPlayers != null)
		{
			for (final String p : allowedPlayers.split(":"))
			{
				final PlayerID id = data.getPlayerList().getPlayerID(p);
				if (id == null)
					System.err.println("gamePlay sequence step: " + data.getSequence().getStep().getName() + " stepProperty: " + GameStep.PROPERTY_combinedTurns + " player: " + p
								+ " DOES NOT EXIST");
				else
					allowedIDs.add(id);
			}
		}
		return allowedIDs;
	}
	
	/**
	 * Resets unit state, such as movement, submerged, transport unload/load, airborne, etc. Normally occurs at end of noncombat move phase.
	 */
	public static boolean isResetUnitState(final GameData data)
	{
		final String prop = data.getSequence().getStep().getProperties().getProperty(GameStep.PROPERTY_resetUnitState);
		if (prop != null)
			return Boolean.parseBoolean(prop);
		if (isNonCombatDelegate(data))
			return true;
		return false;
	}
	
	public static boolean isBid(final GameData data)
	{
		final String prop = data.getSequence().getStep().getProperties().getProperty(GameStep.PROPERTY_bid);
		if (prop != null)
			return Boolean.parseBoolean(prop);
		if (isBidPurchaseDelegate(data))
			return true;
		if (isBidPlaceDelegate(data))
			return true;
		return false;
	}
	
	//
	// private static members for testing default situation based on name of delegate
	//
	private static boolean isNonCombatDelegate(final GameData data)
	{
		if (data.getSequence().getStep().getName().endsWith("NonCombatMove"))
			return true;
		return false;
	}
	
	private static boolean isCombatDelegate(final GameData data)
	{
		if (data.getSequence().getStep().getName().endsWith("NonCombatMove")) // we have to do this check, because otherwise all NonCombatMove delegates become CombatMove delegates too
			return false;
		else if (data.getSequence().getStep().getName().endsWith("CombatMove"))
			return true;
		return false;
	}
	
	private static boolean isBidPurchaseDelegate(final GameData data)
	{
		if (data.getSequence().getStep().getName().endsWith("Bid"))
			return true;
		return false;
	}
	
	private static boolean isBidPlaceDelegate(final GameData data)
	{
		if (data.getSequence().getStep().getName().endsWith("BidPlace"))
			return true;
		return false;
	}
}
