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
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.UnitCollection;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.delegate.Matches;
import games.strategy.util.Match;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author Gansito Frito
 *
 */
class CountryChart
{
    private Collection<Territory> m_terrCollection;
    private GameData m_data;
    private Iterator<Territory> m_terrIterator;
    private Map<Territory, List<Map<UnitType, Integer>>> m_infoMap = new HashMap<Territory, List<Map<UnitType, Integer>>>();
    private PrintGenerationData m_printData;

    protected void saveToFile(PlayerID player, PrintGenerationData printData)
    {
        m_data = printData.getData();
        m_printData=printData;
        m_terrCollection = Match.getMatches(m_data.getMap().getTerritories(), Matches.territoryHasUnitsOwnedBy(player));
        m_terrIterator = m_terrCollection.iterator();
        Iterator<UnitType> availableUnits = m_data.getUnitTypeList().iterator();

        while (m_terrIterator.hasNext())
        {
            Territory currentTerritory = m_terrIterator.next();
            UnitCollection unitsHere = currentTerritory.getUnits();
            List<Map<UnitType, Integer>> unitPairs = new ArrayList<Map<UnitType, Integer>>();
            while (availableUnits.hasNext())
            {
                UnitType currentUnit = availableUnits.next();
                Integer amountHere = unitsHere
                        .getUnitCount(currentUnit, player);
                Map<UnitType, Integer> innerMap = new HashMap<UnitType, Integer>();
                innerMap.put(currentUnit, amountHere);
                unitPairs.add(innerMap);
            }
            m_infoMap.put(currentTerritory, unitPairs);
            availableUnits = m_data.getUnitTypeList().iterator();
        }

        FileWriter countryFileWriter = null;
        try
        {

            File outFile = new File(m_printData.getOutDir(), player
                    .getName()
                    + ".csv");
            countryFileWriter = new FileWriter(outFile, true);

            // Print Title
            int numUnits = m_data.getUnitTypeList().size();
            for (int i = 0; i < numUnits / 2 - 1 + numUnits % 2; i++)
            {
                countryFileWriter.write(",");
            }
            countryFileWriter.write("Setup Chart for the " + player.getName());
            for (int i = 0; i < numUnits / 2 - numUnits % 2; i++)
            {
                countryFileWriter.write(",");
            }
            countryFileWriter.write("\r\n");

            // Print Unit Types

            Iterator<UnitType> unitIterator = m_data.getUnitTypeList()
                    .iterator();
            countryFileWriter.write(",");
            while (unitIterator.hasNext())
            {
                UnitType currentType = unitIterator.next();
                countryFileWriter.write(currentType.getName() + ",");
            }
            countryFileWriter.write("\r\n");

            // Print Territories and Info

             
            m_terrIterator = Match.getMatches(m_data.getMap().getTerritories(), Matches.territoryHasUnitsOwnedBy(player)).iterator();
            while (m_terrIterator.hasNext())
            {
                Territory currentTerritory = m_terrIterator.next();
                countryFileWriter.write(currentTerritory.getName());
                List<Map<UnitType, Integer>> currentList = m_infoMap
                        .get(currentTerritory);
                Iterator<Map<UnitType, Integer>> mapIterator = currentList
                        .iterator();
                while (mapIterator.hasNext())
                {
                    Map<UnitType, Integer> currentMap = mapIterator.next();
                    Iterator<UnitType> uIter = currentMap.keySet().iterator();
                    while (uIter.hasNext())
                    {
                        UnitType uHere = uIter.next();
                        Integer here = currentMap.get(uHere);
                        countryFileWriter.write("," + here);
                    }
                }
                countryFileWriter.write("\r\n");
            }

            countryFileWriter.close();
        } catch (FileNotFoundException e)
        {
            e.printStackTrace();
        } catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }
}
