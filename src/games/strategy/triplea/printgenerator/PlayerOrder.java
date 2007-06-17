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
import games.strategy.engine.data.GameStep;
import games.strategy.engine.data.PlayerID;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Gansito Frito
 * 
 */
public class PlayerOrder
{
    private Iterator<GameStep> m_gameStepIterator;
    private GameData m_data;
    private List<PlayerID> m_playerSet = new ArrayList<PlayerID>();
    private PrintGenerationData m_printData;

    private <E> Set<E> removeDups(Collection<E> c)
    {
        return new LinkedHashSet<E>(c);
    }

    protected void saveToFile(PrintGenerationData printData) throws IOException
    {
        m_data = printData.getData();
        m_printData=printData;
        
        m_gameStepIterator = m_data.getSequence().iterator();
        while (m_gameStepIterator.hasNext())
        {
            GameStep currentStep = m_gameStepIterator.next();
            PlayerID currentPlayerID = currentStep.getPlayerID();

            if (currentPlayerID != null && !currentPlayerID.isNull())
            {
                m_playerSet.add(currentPlayerID);
            }

        }

        FileWriter turnWriter = null;
        m_printData.getOutDir().mkdir();
        File outFile = new File(m_printData.getOutDir(),
                "General Information.csv");
        turnWriter = new FileWriter(outFile, true);

        turnWriter.write("Turn Order\r\n");

        Set<PlayerID> noDuplicates = removeDups(m_playerSet);

        Iterator<PlayerID> playerIterator = noDuplicates.iterator();
        int count=1;
        while (playerIterator.hasNext())
        {
            PlayerID currentPlayerID = playerIterator.next();
            turnWriter.write(count+". "+currentPlayerID.getName() + "\r\n");
            count++;
        }

        turnWriter.close();
    }
}
