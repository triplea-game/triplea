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

package games.strategy.triplea.printgenerator;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.history.HistoryNode;
import games.strategy.triplea.attatchments.UnitAttachment;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @author Gansito Frito
 *
 */
public class InitialSetup
{
    private Iterator<UnitType> m_unitTypeIterator;
    private Iterator<PlayerID> m_playerIterator;
    private Map<UnitType, UnitAttachment> m_unitInfoMap=new HashMap<UnitType, UnitAttachment>();
    private GameData m_data;
    
    private PrintGenerationData m_printData;


    protected InitialSetup()
    {

    }

    /**
     * @param GameData data
     * @param boolean useOriginalState
     */
    protected void run(PrintGenerationData printData, boolean useOriginalState)
    {
        m_data = printData.getData();
        m_printData=printData;
        
        if (useOriginalState)
        {
            HistoryNode root = (HistoryNode) m_data.getHistory().getRoot();
            m_data.getHistory().gotoNode(root);
        }

        m_unitTypeIterator = m_data.getUnitTypeList().iterator();
        while (m_unitTypeIterator.hasNext())
        {
            UnitType currentType = m_unitTypeIterator.next();
            UnitAttachment currentTypeUnitAttachment=UnitAttachment.get(currentType);
            m_unitInfoMap.put(currentType, currentTypeUnitAttachment);
        }
        
        new UnitInformation().saveToFile(m_printData, m_unitInfoMap);
        
        m_playerIterator=m_data.getPlayerList().iterator();
        while(m_playerIterator.hasNext())
        {
            PlayerID currentPlayer=m_playerIterator.next();
            new CountryChart().saveToFile(currentPlayer, m_printData);
            
        }
        new IPCInfo().saveToFile(m_printData);
        try
        {
            new PlayerOrder().saveToFile(m_printData);
            new IPCChart(m_printData).saveToFile();
        } catch (IOException e)
        {
            e.printStackTrace();
        }
        
    }
}
