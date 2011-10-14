package games.strategy.twoIfBySea.delegate;

import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.delegate.AbstractEndTurnDelegate;
import games.strategy.engine.data.*;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attatchments.*;



/**
 * <p>Title: TripleA</p>
 * <p> </p>
 * <p> Copyright (c) 2002</p>
 * <p> </p>
 * @author Sean Bridges
 *
 */

public class EndTurnDelegate extends AbstractEndTurnDelegate
{
	protected boolean m_gameOver = false;

    public EndTurnDelegate()
    {
    }

	@Override
	protected void doNationalObjectivesAndOtherEndTurnEffects(IDelegateBridge bridge)
	{
        GameData data = getData();
        PlayerList playerList = data.getPlayerList();
        PlayerID british = playerList.getPlayerID(Constants.BRITISH);
        PlayerID japanese = playerList.getPlayerID(Constants.JAPANESE);

		// Quick check to see who still owns their own capital
        boolean britain = TerritoryAttachment.getCapital(british, data).getOwner().equals(british);
        boolean japan = TerritoryAttachment.getCapital(japanese, data).getOwner().equals(japanese);

		if(!m_gameOver)
		{
		    if(britain && ! japan)
			{
				m_gameOver = true;
	    		bridge.getHistoryWriter().startEvent("British win.");
			}
			if(!britain && japan)
			{
				m_gameOver = true;
	    		bridge.getHistoryWriter().startEvent("Japanese win.");
			}
		}

	}
}
