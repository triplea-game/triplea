package games.strategy.triplea.printgenerator;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.UnitCollection;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.delegate.Matches;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.triplea.java.collections.CollectionUtils;

@Slf4j
class CountryChart {
  private final Map<Territory, List<Map<UnitType, Integer>>> infoMap = new HashMap<>();
  private final GamePlayer player;

  CountryChart(final GamePlayer player) {
    this.player = player;
  }

  void saveToFile(final PrintGenerationData printData) {
    final Collection<Territory> terrCollection =
        CollectionUtils.getMatches(
            printData.getData().getMap().getTerritories(),
            Matches.territoryHasUnitsOwnedBy(player));
    Iterator<Territory> terrIterator = terrCollection.iterator();
    Iterator<UnitType> availableUnits = printData.getData().getUnitTypeList().iterator();
    while (terrIterator.hasNext()) {
      final Territory currentTerritory = terrIterator.next();
      final UnitCollection unitsHere = currentTerritory.getUnitCollection();
      final List<Map<UnitType, Integer>> unitPairs = new ArrayList<>();
      while (availableUnits.hasNext()) {
        final UnitType currentUnit = availableUnits.next();
        final int amountHere = unitsHere.getUnitCount(currentUnit, player);
        final Map<UnitType, Integer> innerMap = new HashMap<>();
        innerMap.put(currentUnit, amountHere);
        unitPairs.add(innerMap);
      }
      infoMap.put(currentTerritory, unitPairs);
      availableUnits = printData.getData().getUnitTypeList().iterator();
    }
    final Path outFile = printData.getOutDir().resolve(player.getName() + ".csv");
    try (Writer countryFileWriter =
        Files.newBufferedWriter(
            outFile,
            StandardCharsets.UTF_8,
            StandardOpenOption.CREATE,
            StandardOpenOption.APPEND)) {
      // Print Title
      final int numUnits = printData.getData().getUnitTypeList().size();
      for (int i = 0; i < numUnits / 2 - 1 + numUnits % 2; i++) {
        countryFileWriter.write(",");
      }
      countryFileWriter.write("Setup Chart for the " + player.getName());
      for (int i = 0; i < numUnits / 2 - numUnits % 2; i++) {
        countryFileWriter.write(",");
      }
      countryFileWriter.write("\r\n");
      // Print Unit Types
      countryFileWriter.write(",");
      for (final UnitType currentType : printData.getData().getUnitTypeList()) {
        countryFileWriter.write(currentType.getName() + ",");
      }
      countryFileWriter.write("\r\n");
      // Print Territories and Info
      terrIterator =
          CollectionUtils.getMatches(
                  printData.getData().getMap().getTerritories(),
                  Matches.territoryHasUnitsOwnedBy(player))
              .iterator();
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
      log.error("Failed Saving to File " + outFile, e);
    }
  }
}
