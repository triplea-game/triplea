/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package games.strategy.triplea.ui.display;

import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.display.IDisplayBridge;
import games.strategy.engine.framework.IGameLoader;
import games.strategy.engine.gamePlayer.IGamePlayer;
import games.strategy.net.GUID;
import games.strategy.triplea.TripleAPlayer;
import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.delegate.Die;
import games.strategy.triplea.delegate.IBattle.BattleType;
import games.strategy.triplea.ui.TripleAFrame;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 
 * 
 * 
 * @author Sean Bridges
 */
public class TripleaDisplay implements ITripleaDisplay
{
	private IDisplayBridge m_displayBridge;
	private final TripleAFrame m_ui;
	
	/**
	 * @param ui
	 */
	public TripleaDisplay(final TripleAFrame ui)
	{
		m_ui = ui;
	}
	
	/*
	 * @see games.strategy.engine.display.IDisplay#initialize(games.strategy.engine.display.IDisplayBridge)
	 */
	public void initialize(final IDisplayBridge bridge)
	{
		m_displayBridge = bridge;
		m_displayBridge.toString();
	}
	
	/* (non-Javadoc)
	 * @see games.strategy.triplea.ui.display.ITripleaDisplay#showBattle(games.strategy.net.GUID, java.util.List, games.strategy.engine.data.Territory, java.lang.String, java.util.Collection, java.util.Collection)
	 */
	public void showBattle(final GUID battleID, final Territory location, final String battleTitle, final Collection<Unit> attackingUnits, final Collection<Unit> defendingUnits,
				final Collection<Unit> killedUnits, final Collection<Unit> attackingWaitingToDie, final Collection<Unit> defendingWaitingToDie, final Map<Unit, Collection<Unit>> unit_dependents,
				final PlayerID attacker, final PlayerID defender, final boolean isAmphibious, final BattleType battleType)
	{
		m_ui.getBattlePanel().showBattle(battleID, location, battleTitle, attackingUnits, defendingUnits, killedUnits, attackingWaitingToDie, defendingWaitingToDie, unit_dependents, attacker,
					defender, isAmphibious, battleType);
	}
	
	/* (non-Javadoc)
	 * @see games.strategy.triplea.ui.display.ITripleaDisplay#listBattleSteps(games.strategy.net.GUID, java.lang.String, java.util.List)
	 */
	public void listBattleSteps(final GUID battleID, final List<String> steps)
	{
		m_ui.getBattlePanel().listBattle(battleID, steps);
	}
	
	/*
	 * @see games.strategy.triplea.ui.display.ITripleaDisplay#casualtyNotification(java.lang.String, games.strategy.triplea.delegate.DiceRoll, games.strategy.engine.data.PlayerID, java.util.Collection, java.util.Collection, java.util.Map, boolean)
	 */
	public void casualtyNotification(final GUID battleID, final String step, final DiceRoll dice, final PlayerID player, final Collection<Unit> killed, final Collection<Unit> damaged,
				final Map<Unit, Collection<Unit>> dependents)
	{
		m_ui.getBattlePanel().casualtyNotification(step, dice, player, killed, damaged, dependents);
	}
	
	/*
	 * @see games.strategy.triplea.ui.display.ITripleaDisplay#casualtyNotification(java.lang.String, games.strategy.triplea.delegate.DiceRoll, games.strategy.engine.data.PlayerID, java.util.Collection, java.util.Collection, java.util.Map, boolean)
	 */
	public void deadUnitNotification(final GUID battleID, final PlayerID player, final Collection<Unit> killed, final Map<Unit, Collection<Unit>> dependents)
	{
		m_ui.getBattlePanel().deadUnitNotification(player, killed, dependents);
	}
	
	public void changedUnitsNotification(final GUID battleID, final PlayerID player, final Collection<Unit> removedUnits, final Collection<Unit> addedUnits,
				final Map<Unit, Collection<Unit>> dependents)
	{
		m_ui.getBattlePanel().changedUnitsNotification(player, removedUnits, addedUnits, dependents);
	}
	
	/*
	 * @see games.strategy.triplea.ui.display.ITripleaDisplay#casualtyNotification(java.lang.String, games.strategy.triplea.delegate.DiceRoll, games.strategy.engine.data.PlayerID, java.util.Collection, java.util.Collection, java.util.Map, boolean)
	 
	public void scrambleNotification(final GUID battleID, final String step, final PlayerID player, final Collection<Unit> scrambled, final Map<Unit, Collection<Unit>> dependents)
	{
		m_ui.getBattlePanel().scrambleNotification(step, player, scrambled, dependents);
	}
	
	public void notifyScramble(final String shortMessage, final String message, final String step, final PlayerID scramblingPlayer)
	{
		// we just told the game to scramble, so we already know
		if (m_ui.playing(scramblingPlayer))
			return;
		m_ui.getBattlePanel().notifyScramble(shortMessage, message, step, scramblingPlayer);
	}*/

	/* (non-Javadoc)
	 * @see games.strategy.triplea.ui.display.ITripleaDisplay#battleEnd(games.strategy.net.GUID, java.lang.String)
	 */
	public void battleEnd(final GUID battleID, final String message)
	{
		m_ui.getBattlePanel().battleEndMessage(battleID, message);
	}
	
	/* )
	 * @see games.strategy.triplea.ui.display.ITripleaDisplay#bombingResults(games.strategy.net.GUID, int[], int)
	 */
	public void bombingResults(final GUID battleID, final List<Die> dice, final int cost)
	{
		m_ui.getBattlePanel().bombingResults(battleID, dice, cost);
	}
	
	public void notifyRetreat(final String shortMessage, final String message, final String step, final PlayerID retreatingPlayer)
	{
		// we just told the game to retreat, so we already know
		if (m_ui.playing(retreatingPlayer))
			return;
		m_ui.getBattlePanel().notifyRetreat(shortMessage, message, step, retreatingPlayer);
	}
	
	/**
	 * Show dice for the given battle and step
	 * 
	 * @param battleId
	 * @param dice
	 * @param the
	 *            player who must act on the roll, ignore
	 */
	public void notifyDice(final GUID battleId, final DiceRoll dice, final String stepName)
	{
		m_ui.getBattlePanel().showDice(null, dice, stepName);
	}
	
	public void notifyRetreat(final GUID battleId, final Collection<Unit> retreating)
	{
		m_ui.getBattlePanel().notifyRetreat(retreating);
	}
	
	public void gotoBattleStep(final GUID battleId, final String step)
	{
		m_ui.getBattlePanel().gotoStep(battleId, step);
	}
	
	public void shutDown()
	{
		m_ui.stopGame();
	}
	
	public void reportMessageToAll(final String message, final String title, final boolean doNotIncludeHost, final boolean doNotIncludeClients, final boolean doNotIncludeObservers)
	{
		if (doNotIncludeHost && doNotIncludeClients && doNotIncludeObservers)
			return;
		boolean isHost = false;
		boolean isClient = false;
		boolean isObserver = true;
		if (doNotIncludeHost || doNotIncludeClients || doNotIncludeObservers)
		{
			for (final IGamePlayer player : m_ui.GetLocalPlayers())
			{
				isObserver = false; // if we have any local players, we are not an observer
				if (player instanceof TripleAPlayer)
				{
					if (IGameLoader.CLIENT_PLAYER_TYPE.equals(((TripleAPlayer) player).getType()))
						isClient = true;
					else
						isHost = true;
				}
				else
				{
					// AIs are run by the host machine
					isHost = true;
				}
			}
		}
		if ((!doNotIncludeHost && !doNotIncludeClients && !doNotIncludeObservers) || (doNotIncludeHost && !isHost) || (doNotIncludeClients && !isClient) || (doNotIncludeObservers && !isObserver))
		{
			m_ui.notifyMessage(message, title);
		}
	}
	
	public void reportMessageToPlayers(final Collection<PlayerID> playersToSendTo, final Collection<PlayerID> butNotThesePlayers, final String message, final String title)
	{
		if (playersToSendTo == null || playersToSendTo.isEmpty())
			return;
		if (butNotThesePlayers != null)
		{
			for (final PlayerID p : butNotThesePlayers)
			{
				if (m_ui.playing(p))
				{
					return;
				}
			}
		}
		boolean isPlaying = false;
		for (final PlayerID p : playersToSendTo)
		{
			if (m_ui.playing(p))
			{
				isPlaying = true;
				break;
			}
		}
		if (isPlaying)
		{
			m_ui.notifyMessage(message, title);
		}
	}
}
