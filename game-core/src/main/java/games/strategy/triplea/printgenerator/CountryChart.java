package games.strategy.triplea.printgenerator;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
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
import games.strategy.util.CollectionUtils;

class CountryChart {
  private final Map<Territory, List<Map<UnitType, Integer>>> infoMap = new HashMap<>();

  void saveToFile(final PlayerID player, final PrintGenerationData printData) {
    final GameData gameData = printData.getData();
    final Collection<Territory> terrCollection =
        CollectionUtils.getMatches(gameData.getMap().getTerritories(), Matches.territoryHasUnitsOwnedBy(player));
    Iterator<Territory> terrIterator = terrCollection.iterator();
    Iterator<UnitType> availableUnits = gameData.getUnitTypeList().iterator();
    while (terrIterator.hasNext()) {
      final Territory currentTerritory = terrIterator.next();
      final UnitCollection unitsHere = currentTerritory.getUnits();
      final List<Map<UnitType, Integer>> unitPairs = new ArrayList<>();
      while (availableUnits.hasNext()) {
        final UnitType currentUnit = availableUnits.next();
        final int amountHere = unitsHere.getUnitCount(currentUnit, player);
        final Map<UnitType, Integer> innerMap = new HashMap<>();
        innerMap.put(currentUnit, amountHere);
        unitPairs.add(innerMap);
      }
      infoMap.put(currentTerritory, unitPairs);
      availableUnits = gameData.getUnitTypeList().iterator();
    }
    final File outFile = new File(printData.getOutDir(), player.getName() + ".csv");
    try (Writer countryFileWriter = Files.newBufferedWriter(
        outFile.toPath(),
        StandardCharsets.UTF_8,
        StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
      // Print Title
      final int numUnits = gameData.getUnitTypeList().size();
      for (int i = 0; i < (((numUnits / 2) - 1) + (numUnits % 2)); i++) {
        countryFileWriter.write(",");
      }
      countryFileWriter.write("Setup Chart for the " + player.getName());
      for (int i = 0; i < ((numUnits / 2) - (numUnits % 2)); i++) {
        countryFileWriter.write(",");
      }
      countryFileWriter.write("\r\n");
      // Print Unit Types
      countryFileWriter.write(",");
      for (final UnitType currentType : gameData.getUnitTypeList()) {
        countryFileWriter.write(currentType.getName() + ",");
      }
      countryFileWriter.write("\r\n");
      // Print Territories and Info
      terrIterator = CollectionUtils
          .getMatches(gameData.getMap().getTerritories(), Matches.territoryHasUnitsOwnedBy(player)).iterator();
      while (terrIterator.hasNext()) {
        final Territory currentTerritory = terrIterator.next();
        countryFileWriter.write(currentTerritory.getName());
        final List<Map<UnitType, Integer>> currentList = infoMap.get(currentTerritory);
        for (final Map<UnitType, Integer> currentMap : currentList) {
          for (final int here : currentMap.values()) {
            countryFileWriter.write("," + here);
          }
        }
        countryFileWriter.write("\r\n");
      }
    } catch (final IOException e) {
      ClientLogger.logError("Failed Saving to File " + outFile.toString(), e);
    }
  }
}
