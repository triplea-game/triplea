package games.strategy.triplea.ui.display;

import games.strategy.engine.data.*;
import games.strategy.engine.display.IDisplayBridge;
import games.strategy.net.GUID;
import games.strategy.triplea.delegate.DiceRoll;

import java.util.*;

public class DummyDisplay implements ITripleaDisplay
{

    public void showBattle(GUID battleID, Territory location, String battleTitle, Collection<Unit> attackingUnits, Collection<Unit> defendingUnits,
            Map<Unit, Collection<Unit>> dependentUnits, PlayerID attacker, PlayerID defender)
    {
        // TODO Auto-generated method stub

    }

    public void listBattleSteps(GUID battleID, List steps)
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

    public void bombingResults(GUID battleID, int[] dice, int cost)
    {
        // TODO Auto-generated method stub

    }

    public void notifyRetreat(String shortMessage, String message, String step, PlayerID retreatingPlayer)
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

}
