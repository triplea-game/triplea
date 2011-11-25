package games.strategy.triplea.ui.display;

import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.display.IDisplayBridge;
import games.strategy.net.GUID;
import games.strategy.triplea.delegate.DiceRoll;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class DummyDisplay implements ITripleaDisplay
{
	/*
	 * (non-Javadoc)
	 * @see games.strategy.triplea.ui.display.ITripleaDisplay#showBattle(games.strategy.net.GUID, games.strategy.engine.data.Territory, java.lang.String, java.util.Collection, java.util.Collection, java.util.Collection, java.util.Map, games.strategy.engine.data.PlayerID,
	 * games.strategy.engine.data.PlayerID)
	 */
	public void showBattle(final GUID battleID, final Territory location, final String battleTitle, final Collection<Unit> attackingUnits, final Collection<Unit> defendingUnits,
				final Collection<Unit> killedUnits, final Collection<Unit> attackingWaitingToDie, final Collection<Unit> defendingWaitingToDie, final Map<Unit, Collection<Unit>> dependentUnits,
				final PlayerID attacker, final PlayerID defender, final String battleType)
	{
		// TODO Auto-generated method stub
	}
	
	public void listBattleSteps(final GUID battleID, final List<String> steps)
	{
		// TODO Auto-generated method stub
	}
	
	public void battleEnd(final GUID battleID, final String message)
	{
		// TODO Auto-generated method stub
	}
	
	public void casualtyNotification(final GUID battleID, final String step, final DiceRoll dice, final PlayerID player, final Collection<Unit> killed, final Collection<Unit> damaged,
				final Map<Unit, Collection<Unit>> dependents)
	{
		// TODO Auto-generated method stub
	}
	
	public void deadUnitNotification(final GUID battleID, final PlayerID player, final Collection<Unit> killed, final Map<Unit, Collection<Unit>> dependents)
	{
		// TODO Auto-generated method stub
	}
	
	public void changedUnitsNotification(final GUID battleID, final PlayerID player, final Collection<Unit> removedUnits, final Collection<Unit> addedUnits,
				final Map<Unit, Collection<Unit>> dependents)
	{
		// TODO Auto-generated method stub
	}
	
	public void bombingResults(final GUID battleID, final int[] dice, final int cost)
	{
		// TODO Auto-generated method stub
	}
	
	public void notifyRetreat(final String shortMessage, final String message, final String step, final PlayerID retreatingPlayer)
	{
		// TODO Auto-generated method stub
	}
	
	/*public void notifyScramble(final String shortMessage, final String message, final String step, final PlayerID retreatingPlayer)
	{
		// TODO Auto-generated method stub
	}
	
	public void scrambleNotification(final GUID battleID, final String step, final PlayerID player, final Collection<Unit> scrambled, final Map<Unit, Collection<Unit>> dependents)
	{
		// TODO Auto-generated method stub
	}*/

	public void notifyRetreat(final GUID battleId, final Collection<Unit> retreating)
	{
		// TODO Auto-generated method stub
	}
	
	public void notifyDice(final GUID battleId, final DiceRoll dice, final String stepName)
	{
		// TODO Auto-generated method stub
	}
	
	public void gotoBattleStep(final GUID battleId, final String step)
	{
		// TODO Auto-generated method stub
	}
	
	public void initialize(final IDisplayBridge bridge)
	{
		// TODO Auto-generated method stub
	}
	
	public void shutDown()
	{
		// TODO Auto-generated method stub
	}
}
