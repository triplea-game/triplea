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
import games.strategy.engine.display.IDisplay;
import games.strategy.net.GUID;
import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.delegate.Die;
import games.strategy.triplea.delegate.IBattle.BattleType;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 
 * 
 * 
 * @author Sean Bridges
 */
public interface ITripleaDisplay extends IDisplay
{
	/**
	 * Sends a message to all TripleAFrame that have joined the game, possibly including observers.
	 * 
	 * @param message
	 * @param title
	 */
	public void reportMessageToAll(final String message, final String title, final boolean doNotIncludeHost, final boolean doNotIncludeClients, final boolean doNotIncludeObservers);
	
	/**
	 * Sends a message to all TripleAFrame's that are playing AND are controlling one or more of the players listed but NOT any of the players listed as butNotThesePlayers.
	 * (No message to any observers or players not in the list.)
	 * 
	 * @param playersToSendTo
	 * @param butNotThesePlayers
	 * @param message
	 * @param title
	 */
	public void reportMessageToPlayers(final Collection<PlayerID> playersToSendTo, final Collection<PlayerID> butNotThesePlayers, final String message, final String title);
	
	/**
	 * Display info about the battle.
	 * This is the first message to be displayed in a battle
	 * 
	 * @param battleID
	 *            - a unique id for the battle
	 * @param location
	 *            - where the battle occurs
	 * @param battleTitle
	 *            - the title of the battle
	 * @param attackingUnits
	 *            - attacking units
	 * @param defendingUnits
	 *            - defending units
	 * @param killedUnits
	 *            - killed units
	 * @param dependentUnits
	 *            - unit dependencies, maps Unit->Collection of units
	 * @param attacker
	 *            - PlayerID of attacker
	 * @param defender
	 *            - PlayerID of defender
	 * @param isAmphibious
	 */
	public void showBattle(GUID battleID, Territory location, String battleTitle, Collection<Unit> attackingUnits, Collection<Unit> defendingUnits, Collection<Unit> killedUnits,
				Collection<Unit> attackingWaitingToDie, Collection<Unit> defendingWaitingToDie, Map<Unit, Collection<Unit>> dependentUnits, final PlayerID attacker,
				final PlayerID defender, final boolean isAmphibious, final BattleType battleType, final Collection<Unit> amphibiousLandAttackers);
	
	/**
	 * 
	 * @param battleID
	 *            - the battle we are listing steps for
	 * @param currentStep
	 *            - the current step
	 * @param steps
	 *            - a collection of strings denoting all steps in the battle
	 */
	public void listBattleSteps(GUID battleID, List<String> steps);
	
	/**
	 * The given battle has ended.
	 */
	public void battleEnd(GUID battleID, String message);
	
	/**
	 * Notify that the casualties occurred
	 * 
	 */
	public void casualtyNotification(GUID battleID, String step, DiceRoll dice, PlayerID player, Collection<Unit> killed, Collection<Unit> damaged, Map<Unit, Collection<Unit>> dependents);
	
	/**
	 * Notify that the casualties occurred, and only the casualty
	 */
	public void deadUnitNotification(GUID battleID, PlayerID player, Collection<Unit> dead, Map<Unit, Collection<Unit>> dependents);
	
	public void changedUnitsNotification(GUID battleID, PlayerID player, Collection<Unit> removedUnits, Collection<Unit> addedUnits, Map<Unit, Collection<Unit>> dependents);
	
	/*
	 * Notify that the casualties occurred
	 * 
	public void scrambleNotification(GUID battleID, String step, PlayerID player, Collection<Unit> scrambled, Map<Unit, Collection<Unit>> dependents);
	
	public void notifyScramble(String shortMessage, String message, String step, PlayerID scramblingPlayer);*/
	
	/**
	 * Notification of the results of a bombing raid
	 */
	public void bombingResults(GUID battleID, List<Die> dice, int cost);
	
	/**
	 * Notify that the given player has retreated some or all of his units.
	 * 
	 * @param shortMessage
	 * @param message
	 * @param step
	 */
	public void notifyRetreat(String shortMessage, String message, String step, PlayerID retreatingPlayer);
	
	public void notifyRetreat(GUID battleId, Collection<Unit> retreating);
	
	/**
	 * Show dice for the given battle and step
	 * 
	 * @param battleId
	 * @param dice
	 */
	public void notifyDice(GUID battleId, DiceRoll dice, String stepName);
	
	public void gotoBattleStep(GUID battleId, String step);
}
