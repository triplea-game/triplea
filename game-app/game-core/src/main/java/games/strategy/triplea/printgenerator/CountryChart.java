package games.strategy.triplea.printgenerator;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.UnitCollection;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.UnitTypeList;
import games.strategy.triplea.delegate.Matches;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.triplea.java.collections.CollectionUtils;

@Slf4j
class CountryChart extends InfoForFile {
  private final Map<Territory, List<Map<UnitType, Integer>>> infoMap = new HashMap<>();
  private final GamePlayer player;
  GameData gameData;

  CountryChart(Path outDir, final GamePlayer player) {
    super(outDir.resolve((player.getName() + ".csv")));
    this.player = player;
  }

  @Override
  protected void gatherDataBeforeWriting(PrintGenerationData printData) {
    gameData = printData.getData();
    final UnitTypeList unitTypeList = printData.getData().getUnitTypeList();
    CollectionUtils.getMatches(
            printData.getData().getMap().getTerritories(), Matches.territoryHasUnitsOwnedBy(player))
        .forEach(
            currentTerritory -> {
              final UnitCollection unitsHere = currentTerritory.getUnitCollection();
              final List<Map<UnitType, Integer>> unitPairs = new ArrayList<>();
              unitTypeList.forEach(
                  currentUnit -> {
                    final int amountHere = unitsHere.getUnitCount(currentUnit, player);
                    final Map<UnitType, Integer> innerMap = new HashMap<>();
                    innerMap.put(currentUnit, amountHere);
                    unitPairs.add(innerMap);
                  });
              infoMap.put(currentTerritory, unitPairs);
            });
  }

  @Override
  protected void writeIntoFile(Writer writer) throws IOException {
    // Print Title
    final int numUnits = gameData.getUnitTypeList().size();
    for (int i = 0; i < numUnits / 2 - 1 + numUnits % 2; i++) {
      writer.write(",");
    }
    writer.write("Setup Chart for the " + player.getName());
    for (int i = 0; i < numUnits / 2 - numUnits % 2; i++) {
      writer.write(",");
    }
    writer.write("\r\n");
    // Print Unit Types
    writer.write(",");
    for (final UnitType currentType : gameData.getUnitTypeList()) {
      writer.write(currentType.getName() + ",");
    }
    writer.write("\r\n");
    // Print Territories and Info
    for (final Territory currentTerritory :
        CollectionUtils.getMatches(
            gameData.getMap().getTerritories(), Matches.territoryHasUnitsOwnedBy(player))) {
      writer.write(currentTerritory.getName());
      final List<Map<UnitType, Integer>> currentList = infoMap.get(currentTerritory);
      for (final Map<UnitType, Integer> currentMap : currentList) {
        for (final int here : currentMap.values()) {
          writer.write("," + here);
        }
      }
      writer.write("\r\n");
    }
  }
}
