/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

/*
 * EndTurnDelegate.java
 *
 * Created on November 2, 2001, 12:30 PM
 */

package games.strategy.triplea.delegate;

import java.util.*;

import games.strategy.engine.data.*;
import games.strategy.engine.delegate.*;
import games.strategy.engine.message.*;
import games.strategy.engine.transcript.*;

import games.strategy.triplea.Constants;
import games.strategy.triplea.attatchments.*;

/**
 *
 * @author  Sean Bridges
 * @version 1.0
 *
 * At the end of the turn collect income.
 */
public class EndTurnDelegate extends AbstractEndTurnDelegate
{

	protected void checkForWinner(DelegateBridge bridge)
	{
		//only notify once
		if(m_gameOver)
			return;


		PlayerID russians = m_data.getPlayerList().getPlayerID(Constants.RUSSIANS);
		PlayerID germans = m_data.getPlayerList().getPlayerID(Constants.GERMANS);
		PlayerID british = m_data.getPlayerList().getPlayerID(Constants.BRITISH);
		PlayerID japanese = m_data.getPlayerList().getPlayerID(Constants.JAPANESE);
		PlayerID americans = m_data.getPlayerList().getPlayerID(Constants.AMERICANS);

		// Quick check to see who still owns their own capital
		boolean russia = TerritoryAttatchment.getCapital(russians, m_data).getOwner().equals(russians);
		boolean germany = TerritoryAttatchment.getCapital(germans, m_data).getOwner().equals(germans);
		boolean britain = TerritoryAttatchment.getCapital(british, m_data).getOwner().equals(british);
		boolean japan = TerritoryAttatchment.getCapital(japanese, m_data).getOwner().equals(japanese);
		boolean america = TerritoryAttatchment.getCapital(americans, m_data).getOwner().equals(americans);

		int count = 0;
		if (!russia) count++;
		if (!britain) count++;
		if (!america) count++;

		if ( germany && japan && count >=2)
		{
			m_gameOver = true;
			bridge.getTranscript().write("Axis achieve a military victory", TranscriptMessage.PRIORITY_CHANNEL);
		}

	 	if ( russia && !germany && britain && !japan && america)
		{
			m_gameOver = true;
			bridge.getTranscript().write("Allies achieve a military victory", TranscriptMessage.PRIORITY_CHANNEL);
		}

	}


}
