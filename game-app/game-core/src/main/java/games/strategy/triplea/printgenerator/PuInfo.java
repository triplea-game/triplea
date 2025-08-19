package games.strategy.triplea.printgenerator;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.PlayerList;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.ResourceList;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

class PuInfo extends InfoForFile {
  private final Map<GamePlayer, Map<Resource, Integer>> infoMap = new HashMap<>();
  private PlayerList gamePlayers = null;
  private ResourceList resourceList = null;

  @Override
  protected void gatherDataBeforeWriting(PrintGenerationData printData) {
    gamePlayers = printData.getData().getPlayerList();
    resourceList = printData.getData().getResourceList();
    for (final GamePlayer currentPlayer : gamePlayers) {
      infoMap.put(
          currentPlayer,
          printData.getData().getResourceList().getResources().stream()
              .collect(
                  Collectors.toMap(
                      Function.identity(), currentPlayer.getResources()::getQuantity)));
    }
  }

  @Override
  protected void writeIntoFile(Writer writer) throws IOException {
    // Print Title
    final int numResources = resourceList.size();
    for (int i = 0; i < numResources / 2 - 1 + numResources % 2; i++) {
      writer.write(DELIMITER);
    }
    writer.write("Resource Chart");
    for (int i = 0; i < numResources / 2 - numResources % 2; i++) {
      writer.write(DELIMITER);
    }
    writer.write(LINE_SEPARATOR);
    // Print Resources
    writer.write(DELIMITER);
    for (final Resource currentResource : resourceList.getResources()) {
      writer.write(currentResource.getName() + DELIMITER);
    }
    writer.write(LINE_SEPARATOR);
    // Print Player's and Resource Amount's
    for (final GamePlayer currentPlayer : gamePlayers) {
      writer.write(currentPlayer.getName());
      final Map<Resource, Integer> resourceMap = infoMap.get(currentPlayer);
      for (final int amountResource : resourceMap.values()) {
        writer.write(DELIMITER + amountResource);
      }
      writer.write(LINE_SEPARATOR);
    }
    writer.write(LINE_SEPARATOR);
  }
}
