package games.strategy.triplea.printgenerator;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import games.strategy.debug.ClientLogger;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.UnitCollection;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.delegate.Matches;
import games.strategy.util.Match;

class CountryChart {
  private Collection<Territory> m_terrCollection;
  private GameData m_data;
  private Iterator<Territory> m_terrIterator;
  private final Map<Territory, List<Map<UnitType, Integer>>> m_infoMap =
      new HashMap<Territory, List<Map<UnitType, Integer>>>();
  private PrintGenerationData m_printData;

  protected void saveToFile(final PlayerID player, final PrintGenerationData printData) {
    m_data = printData.getData();
    m_printData = printData;
    m_terrCollection = Match.getMatches(m_data.getMap().getTerritories(), Matches.territoryHasUnitsOwnedBy(player));
    m_terrIterator = m_terrCollection.iterator();
    Iterator<UnitType> availableUnits = m_data.getUnitTypeList().iterator();
    while (m_terrIterator.hasNext()) {
      final Territory currentTerritory = m_terrIterator.next();
      final UnitCollection unitsHere = currentTerritory.getUnits();
      final List<Map<UnitType, Integer>> unitPairs = new ArrayList<Map<UnitType, Integer>>();
      while (availableUnits.hasNext()) {
        final UnitType currentUnit = availableUnits.next();
        final Integer amountHere = unitsHere.getUnitCount(currentUnit, player);
        final Map<UnitType, Integer> innerMap = new HashMap<UnitType, Integer>();
        innerMap.put(currentUnit, amountHere);
        unitPairs.add(innerMap);
      }
      m_infoMap.put(currentTerritory, unitPairs);
      availableUnits = m_data.getUnitTypeList().iterator();
    }
    FileWriter countryFileWriter = null;
    final File outFile = new File(m_printData.getOutDir(), player.getName() + ".csv");
    try {
      countryFileWriter = new FileWriter(outFile, true);
      // Print Title
      final int numUnits = m_data.getUnitTypeList().size();
      for (int i = 0; i < numUnits / 2 - 1 + numUnits % 2; i++) {
        countryFileWriter.write(",");
      }
      countryFileWriter.write("Setup Chart for the " + player.getName());
      for (int i = 0; i < numUnits / 2 - numUnits % 2; i++) {
        countryFileWriter.write(",");
      }
      countryFileWriter.write("\r\n");
      // Print Unit Types
      final Iterator<UnitType> unitIterator = m_data.getUnitTypeList().iterator();
      countryFileWriter.write(",");
      while (unitIterator.hasNext()) {
        final UnitType currentType = unitIterator.next();
        countryFileWriter.write(currentType.getName() + ",");
      }
      countryFileWriter.write("\r\n");
      // Print Territories and Info
      m_terrIterator =
          Match.getMatches(m_data.getMap().getTerritories(), Matches.territoryHasUnitsOwnedBy(player)).iterator();
      while (m_terrIterator.hasNext()) {
        final Territory currentTerritory = m_terrIterator.next();
        countryFileWriter.write(currentTerritory.getName());
        final List<Map<UnitType, Integer>> currentList = m_infoMap.get(currentTerritory);
        for (Map<UnitType, Integer> currentMap : currentList) {
          for (UnitType uHere : currentMap.keySet()) {
            final Integer here = currentMap.get(uHere);
            countryFileWriter.write("," + here);
          }
        }
        countryFileWriter.write("\r\n");
      }
      countryFileWriter.close();
    } catch (final IOException e) {
      ClientLogger.logError("Failed Saving to File " + outFile.toString(), e);
    }
  }
}
