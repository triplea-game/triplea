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
    private static Collection<Territory> s_terrCollection;
    private static GameData s_data;
    private static Iterator<Territory> s_terrIterator;
    private static Map<Territory, List<Map<UnitType, Integer>>> s_infoMap = new HashMap<Territory, List<Map<UnitType, Integer>>>();

    protected static void saveToFile(PlayerID player, GameData data)
    {
        s_data = data;
        s_terrCollection = Match.getMatches(s_data.getMap().getTerritories(), Matches.territoryHasUnitsOwnedBy(player));
        s_terrIterator = s_terrCollection.iterator();
        Iterator<UnitType> availableUnits = s_data.getUnitTypeList().iterator();

        while (s_terrIterator.hasNext())
        {
            Territory currentTerritory = s_terrIterator.next();
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
            s_infoMap.put(currentTerritory, unitPairs);
            availableUnits = s_data.getUnitTypeList().iterator();
        }

        FileWriter countryFileWriter = null;
        try
        {

            File outFile = new File(PrintGenerationData.getOutDir(), player
                    .getName()
                    + ".csv");
            countryFileWriter = new FileWriter(outFile, true);

            // Print Title
            int numUnits = s_data.getUnitTypeList().size();
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

            Iterator<UnitType> unitIterator = s_data.getUnitTypeList()
                    .iterator();
            countryFileWriter.write(",");
            while (unitIterator.hasNext())
            {
                UnitType currentType = unitIterator.next();
                countryFileWriter.write(currentType.getName() + ",");
            }
            countryFileWriter.write("\r\n");

            // Print Territories and Info

             
            s_terrIterator = Match.getMatches(s_data.getMap().getTerritories(), Matches.territoryHasUnitsOwnedBy(player)).iterator();
            while (s_terrIterator.hasNext())
            {
                Territory currentTerritory = s_terrIterator.next();
                countryFileWriter.write(currentTerritory.getName());
                List<Map<UnitType, Integer>> currentList = s_infoMap
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
