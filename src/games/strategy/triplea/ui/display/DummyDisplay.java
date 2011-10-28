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

	public void showBattle(GUID battleID, Territory location, String battleTitle, Collection<Unit> attackingUnits, Collection<Unit> defendingUnits, final Collection<Unit> killedUnits,
				final Collection<Unit> attackingWaitingToDie, final Collection<Unit> defendingWaitingToDie, Map<Unit, Collection<Unit>> dependentUnits, PlayerID attacker, PlayerID defender)
	{
		// TODO Auto-generated method stub
		
	}
	
	public void listBattleSteps(GUID battleID, List<String> steps)
	{
		// TODO Auto-generated method stub
		
	}
	
	public void battleEnd(GUID battleID, String message)
	{
		// TODO Auto-generated method stub
		
	}
	
	public void casualtyNotification(GUID battleID, String step, DiceRoll dice, PlayerID player, Collection<Unit> killed, Collection<Unit> damaged,
				Map<Unit, Collection<Unit>> dependents)
	{
		// TODO Auto-generated method stub
		
	}
	
	public void deadUnitNotification(GUID battleID, PlayerID player, Collection<Unit> killed, Map<Unit, Collection<Unit>> dependents)
	{
		// TODO Auto-generated method stub
		
	}
	
	public void bombingResults(GUID battleID, int[] dice, int cost)
	{
		// TODO Auto-generated method stub
		
	}
	
	public void notifyRetreat(String shortMessage, String message, String step, PlayerID retreatingPlayer)
	{
		// TODO Auto-generated method stub
		
	}
	
	public void notifyScramble(String shortMessage, String message, String step, PlayerID retreatingPlayer)
	{
		// TODO Auto-generated method stub
		
	}
	
	public void notifyRetreat(GUID battleId, Collection<Unit> retreating)
	{
		// TODO Auto-generated method stub
		
	}
	
	public void notifyDice(GUID battleId, DiceRoll dice, String stepName)
	{
		// TODO Auto-generated method stub
		
	}
	
	public void gotoBattleStep(GUID battleId, String step)
	{
		// TODO Auto-generated method stub
		
	}
	
	public void initialize(IDisplayBridge bridge)
	{
		// TODO Auto-generated method stub
		
	}
	
	public void shutDown()
	{
		// TODO Auto-generated method stub
		
	}
	
	public void scrambleNotification(GUID battleID, String step,
				PlayerID player, Collection<Unit> scrambled,
				Map<Unit, Collection<Unit>> dependents)
	{
		// TODO Auto-generated method stub
		
	}
	
}
