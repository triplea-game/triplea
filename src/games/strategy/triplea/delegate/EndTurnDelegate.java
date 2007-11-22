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

import java.io.Serializable;

import games.strategy.engine.data.PlayerID;
import games.strategy.engine.delegate.AutoSave;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attatchments.PlayerAttachment;
import games.strategy.triplea.attatchments.TerritoryAttachment;

/**
 *
 * @author  Sean Bridges
 * @version 1.0
 *
 * At the end of the turn collect income.
 */
@AutoSave(afterStepEnd=true)
public class EndTurnDelegate extends AbstractEndTurnDelegate
{

    protected void checkForWinner(IDelegateBridge bridge)
    {
        //only notify once
        if(m_gameOver)
            return;

        PlayerID russians = m_data.getPlayerList().getPlayerID(Constants.RUSSIANS);
        PlayerID germans = m_data.getPlayerList().getPlayerID(Constants.GERMANS);
        PlayerID british = m_data.getPlayerList().getPlayerID(Constants.BRITISH);
        PlayerID japanese = m_data.getPlayerList().getPlayerID(Constants.JAPANESE);
        PlayerID americans = m_data.getPlayerList().getPlayerID(Constants.AMERICANS);


                if(m_data.getProperties().get(Constants.PACIFIC_EDITION, false))
                {
                    PlayerAttachment pa = PlayerAttachment.get(japanese);

                    
                    if(pa != null && Integer.parseInt(pa.getVps()) >= 22)
                    {
                        m_gameOver = true;
                        bridge.getHistoryWriter().startEvent("Axis achieve VP victory");
                        return;
                    } 
                } 

        if(m_data.getProperties().get(Constants.FOURTH_EDITION, false))
            return;

        
        if(germans == null || russians == null || british == null || japanese == null || americans == null)
            return;
        
        // Quick check to see who still owns their own capital
        boolean russia = TerritoryAttachment.getCapital(russians, m_data).getOwner().equals(russians);
        boolean germany = TerritoryAttachment.getCapital(germans, m_data).getOwner().equals(germans);
        boolean britain = TerritoryAttachment.getCapital(british, m_data).getOwner().equals(british);
        boolean japan = TerritoryAttachment.getCapital(japanese, m_data).getOwner().equals(japanese);
        boolean america = TerritoryAttachment.getCapital(americans, m_data).getOwner().equals(americans);


        int count = 0;
        if (!russia) count++;
        if (!britain) count++;
        if (!america) count++;

        if ( germany && japan && count >=2)
        {
            m_gameOver = true;
            bridge.getHistoryWriter().startEvent("Axis achieve a military victory");
        }

        if ( russia && !germany && britain && !japan && america)
        {
            m_gameOver = true;
            bridge.getHistoryWriter().startEvent("Allies achieve a military victory");
        }

    }
}
