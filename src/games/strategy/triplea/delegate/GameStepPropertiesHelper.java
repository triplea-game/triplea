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
		final boolean skipPosting;
		data.acquireReadLock();
		try
		{
			skipPosting = Boolean.parseBoolean(data.getSequence().getStep().getProperties().getProperty(GameStep.PROPERTY_skipPosting, "false"));
		} finally
		{
			data.releaseReadLock();
		}
		return skipPosting;
	}
	
	/**
	 * What players is this turn summary for? If more than 1 player, whose phases are touching or intermeshed, then we will summarize for all those phases.
	 * 
	 * @return colon separated list of player names. could be empty. can be null if not set.
	 */
	public static Set<PlayerID> getTurnSummaryPlayers(final GameData data)
	{
		final Set<PlayerID> allowedIDs;
		data.acquireReadLock();
		try
		{
			final String allowedPlayers = data.getSequence().getStep().getProperties().getProperty(GameStep.PROPERTY_turnSummaryPlayers);// parse allowed players
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
		} finally
		{
			data.releaseReadLock();
		}
		return allowedIDs;
	}
	
	/**
	 * For various things related to movement validation.
	 */
	public static boolean isAirborneMove(final GameData data)
	{
		final boolean isAirborneMove;
		data.acquireReadLock();
		try
		{
			final String prop = data.getSequence().getStep().getProperties().getProperty(GameStep.PROPERTY_airborneMove);
			if (prop != null)
				isAirborneMove = Boolean.parseBoolean(prop);
			else if (isAirborneDelegate(data))
				isAirborneMove = true;
			else
				isAirborneMove = false;
		} finally
		{
			data.releaseReadLock();
		}
		return isAirborneMove;
	}
	
	/**
	 * For various things related to movement validation.
	 */
	public static boolean isCombatMove(final GameData data, final boolean doNotThrowErrorIfNotMoveDelegate)
	{
		final boolean isCombatMove;
		data.acquireReadLock();
		try
		{
			final String prop = data.getSequence().getStep().getProperties().getProperty(GameStep.PROPERTY_combatMove);
			if (prop != null)
				isCombatMove = Boolean.parseBoolean(prop);
			else if (isCombatDelegate(data))
				isCombatMove = true;
			else if (isNonCombatDelegate(data))
				isCombatMove = false;
			else if (doNotThrowErrorIfNotMoveDelegate)
				isCombatMove = false;
			else
				throw new IllegalStateException("Cannot determine combat or not: " + data.getSequence().getStep().getName());
		} finally
		{
			data.releaseReadLock();
		}
		return isCombatMove;
	}
	
	/**
	 * For various things related to movement validation.
	 */
	public static boolean isNonCombatMove(final GameData data, final boolean doNotThrowErrorIfNotMoveDelegate)
	{
		final boolean isNonCombatMove;
		data.acquireReadLock();
		try
		{
			final String prop = data.getSequence().getStep().getProperties().getProperty(GameStep.PROPERTY_nonCombatMove);
			if (prop != null)
				isNonCombatMove = Boolean.parseBoolean(prop);
			else if (isNonCombatDelegate(data))
				isNonCombatMove = true;
			else if (isCombatDelegate(data))
				isNonCombatMove = false;
			else if (doNotThrowErrorIfNotMoveDelegate)
				isNonCombatMove = false;
			else
				throw new IllegalStateException("Cannot determine combat or not: " + data.getSequence().getStep().getName());
		} finally
		{
			data.releaseReadLock();
		}
		return isNonCombatMove;
	}
	
	/**
	 * Fire rockets after phase is over. Normally would occur after combat move for WW2v2 and WW2v3, and after noncombat move for WW2v1.
	 */
	public static boolean isFireRockets(final GameData data)
	{
		final boolean isFireRockets;
		data.acquireReadLock();
		try
		{
			final String prop = data.getSequence().getStep().getProperties().getProperty(GameStep.PROPERTY_fireRockets);
			if (prop != null)
				isFireRockets = Boolean.parseBoolean(prop);
			else if (games.strategy.triplea.Properties.getWW2V2(data) || games.strategy.triplea.Properties.getWW2V3(data))
			{
				if (isCombatDelegate(data))
					isFireRockets = true;
				else
					isFireRockets = false;
			}
			else if (isNonCombatDelegate(data))
			{
				isFireRockets = true;
			}
			else
				isFireRockets = false;
		} finally
		{
			data.releaseReadLock();
		}
		return isFireRockets;
	}
	
	/**
	 * Repairs damaged units. Normally would occur at either start of combat move or end of turn, depending.
	 */
	public static boolean isRepairUnits(final GameData data)
	{
		final boolean isRepairUnits;
		data.acquireReadLock();
		try
		{
			final boolean repairAtStartAndOnlyOwn = games.strategy.triplea.Properties.getBattleshipsRepairAtBeginningOfRound(data);
			final boolean repairAtEndAndAll = games.strategy.triplea.Properties.getBattleshipsRepairAtEndOfRound(data);
			// if both are off, we do no repairing, no matter what
			if (!repairAtStartAndOnlyOwn && !repairAtEndAndAll)
				isRepairUnits = false;
			else
			{
				final String prop = data.getSequence().getStep().getProperties().getProperty(GameStep.PROPERTY_repairUnits);
				if (prop != null)
					isRepairUnits = Boolean.parseBoolean(prop);
				else if (isCombatDelegate(data) && repairAtStartAndOnlyOwn)
					isRepairUnits = true;
				else if (data.getSequence().getStep().getName().endsWith("EndTurn") && repairAtEndAndAll)
					isRepairUnits = true;
				else
					isRepairUnits = false;
			}
		} finally
		{
			data.releaseReadLock();
		}
		return isRepairUnits;
	}
	
	/**
	 * Resets then gives bonus movement. Normally would occur at the start of combat movement phase.
	 */
	public static boolean isGiveBonusMovement(final GameData data)
	{
		final boolean isBonus;
		data.acquireReadLock();
		try
		{
			final String prop = data.getSequence().getStep().getProperties().getProperty(GameStep.PROPERTY_giveBonusMovement);
			if (prop != null)
				isBonus = Boolean.parseBoolean(prop);
			else if (isCombatDelegate(data))
				isBonus = true;
			else
				isBonus = false;
		} finally
		{
			data.releaseReadLock();
		}
		return isBonus;
	}
	
	/**
	 * Kills all air that can not land. Normally would occur both at the end of noncombat movement and also at end of placement phase.
	 */
	public static boolean isRemoveAirThatCanNotLand(final GameData data)
	{
		final boolean isRemoveAir;
		data.acquireReadLock();
		try
		{
			final String prop = data.getSequence().getStep().getProperties().getProperty(GameStep.PROPERTY_removeAirThatCanNotLand);
			if (prop != null)
				isRemoveAir = Boolean.parseBoolean(prop);
			else if (data.getSequence().getStep().getDelegate() != null && NoAirCheckPlaceDelegate.class.equals(data.getSequence().getStep().getDelegate().getClass()))
				isRemoveAir = false;
			else if (isNonCombatDelegate(data))
				isRemoveAir = true;
			else if (data.getSequence().getStep().getName().endsWith("Place"))
				isRemoveAir = true;
			else
				isRemoveAir = false;
		} finally
		{
			data.releaseReadLock();
		}
		return isRemoveAir;
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
		final Set<PlayerID> allowedIDs = new HashSet<PlayerID>();
		data.acquireReadLock();
		try
		{
			final String allowedPlayers = data.getSequence().getStep().getProperties().getProperty(GameStep.PROPERTY_combinedTurns);// parse allowed players
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
		} finally
		{
			data.releaseReadLock();
		}
		return allowedIDs;
	}
	
	/**
	 * Resets unit state, such as movement, submerged, transport unload/load, airborne, etc. Normally does not occur.
	 */
	public static boolean isResetUnitStateAtStart(final GameData data)
	{
		final boolean isReset;
		data.acquireReadLock();
		try
		{
			final String prop = data.getSequence().getStep().getProperties().getProperty(GameStep.PROPERTY_resetUnitStateAtStart);
			if (prop != null)
				isReset = Boolean.parseBoolean(prop);
			else
				isReset = false;
		} finally
		{
			data.releaseReadLock();
		}
		return isReset;
	}

	/**
	 * Resets unit state, such as movement, submerged, transport unload/load, airborne, etc. Normally occurs at end of noncombat move phase.
	 */
	public static boolean isResetUnitStateAtEnd(final GameData data)
	{
		final boolean isReset;
		data.acquireReadLock();
		try
		{
			final String prop = data.getSequence().getStep().getProperties().getProperty(GameStep.PROPERTY_resetUnitStateAtEnd);
			if (prop != null)
				isReset = Boolean.parseBoolean(prop);
			else if (isNonCombatDelegate(data))
				isReset = true;
			else
				isReset = false;
		} finally
		{
			data.releaseReadLock();
		}
		return isReset;
	}
	
	public static boolean isBid(final GameData data)
	{
		final boolean isBid;
		data.acquireReadLock();
		try
		{
			final String prop = data.getSequence().getStep().getProperties().getProperty(GameStep.PROPERTY_bid);
			if (prop != null)
				isBid = Boolean.parseBoolean(prop);
			else if (isBidPurchaseDelegate(data))
				isBid = true;
			else if (isBidPlaceDelegate(data))
				isBid = true;
			else
				isBid = false;
		} finally
		{
			data.releaseReadLock();
		}
		return isBid;
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
	
	private static boolean isAirborneDelegate(final GameData data)
	{
		if (data.getSequence().getStep().getName().endsWith("AirborneCombatMove")) // AirborneCombatMove is ALSO a combat move, it is just a special combat move
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
