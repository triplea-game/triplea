package games.strategy.twoIfBySea.delegate;

import games.strategy.engine.delegate.DelegateBridge;
import games.strategy.triplea.delegate.AbstractEndTurnDelegate;
import games.strategy.engine.data.*;
import games.strategy.engine.transcript.*;
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

	protected void checkForWinner(DelegateBridge bridge)
	{
		PlayerID british = m_data.getPlayerList().getPlayerID(Constants.BRITISH);
		PlayerID japanese = m_data.getPlayerList().getPlayerID(Constants.JAPANESE);

		// Quick check to see who still owns their own capital
		boolean britain = TerritoryAttatchment.getCapital(british, m_data).getOwner().equals(british);
		boolean japan = TerritoryAttatchment.getCapital(japanese, m_data).getOwner().equals(japanese);

		if(!m_gameOver)
		{
		    if(britain && ! japan)
			{
				m_gameOver = true;
	    		bridge.getTranscript().write("British win.", TranscriptMessage.PRIORITY_CHANNEL);
			}
			if(!britain && japan)
			{
				m_gameOver = true;
	    		bridge.getTranscript().write("Japanese win.", TranscriptMessage.PRIORITY_CHANNEL);
			}
		}

	}
}
