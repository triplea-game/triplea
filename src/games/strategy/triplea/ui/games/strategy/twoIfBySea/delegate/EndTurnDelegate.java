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

    public EndTurnDelegate()
    {
    }

	protected void checkForWinner(IDelegateBridge bridge)
	{
		PlayerID british = m_data.getPlayerList().getPlayerID(Constants.BRITISH);
		PlayerID japanese = m_data.getPlayerList().getPlayerID(Constants.JAPANESE);

		// Quick check to see who still owns their own capital
		boolean britain = TerritoryAttachment.getCapital(british, m_data).getOwner().equals(british);
		boolean japan = TerritoryAttachment.getCapital(japanese, m_data).getOwner().equals(japanese);

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
