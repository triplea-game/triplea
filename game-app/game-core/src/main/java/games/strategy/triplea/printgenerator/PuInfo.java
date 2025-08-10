package games.strategy.triplea.printgenerator;

import static games.strategy.triplea.printgenerator.UnitInformation.FILE_NAME_GENERAL_INFORMATION_CSV;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Resource;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
class PuInfo {
  private final Map<GamePlayer, Map<Resource, Integer>> infoMap = new HashMap<>();

  void saveToFile(final PrintGenerationData printData) {
    for (final GamePlayer currentPlayer : printData.getData().getPlayerList()) {
      infoMap.put(
          currentPlayer,
          printData.getData().getResourceList().getResources().stream()
              .collect(
                  Collectors.toMap(
                      Function.identity(), currentPlayer.getResources()::getQuantity)));
    }
    try {
      final Path outFile = printData.getOutDir().resolve(FILE_NAME_GENERAL_INFORMATION_CSV);
      try (Writer resourceWriter =
          Files.newBufferedWriter(
              outFile,
              StandardCharsets.UTF_8,
              StandardOpenOption.CREATE,
              StandardOpenOption.APPEND)) {
        // Print Title
        final int numResources = printData.getData().getResourceList().size();
        for (int i = 0; i < numResources / 2 - 1 + numResources % 2; i++) {
          resourceWriter.write(",");
        }
        resourceWriter.write("Resource Chart");
        for (int i = 0; i < numResources / 2 - numResources % 2; i++) {
          resourceWriter.write(",");
        }
        resourceWriter.write("\r\n");
        // Print Resources
        resourceWriter.write(",");
        for (final Resource currentResource :
            printData.getData().getResourceList().getResources()) {
          resourceWriter.write(currentResource.getName() + ",");
        }
        resourceWriter.write("\r\n");
        // Print Player's and Resource Amount's
        for (final GamePlayer currentPlayer : printData.getData().getPlayerList()) {
          resourceWriter.write(currentPlayer.getName());
          final Map<Resource, Integer> resourceMap = infoMap.get(currentPlayer);
          for (final int amountResource : resourceMap.values()) {
            resourceWriter.write("," + amountResource);
          }
          resourceWriter.write("\r\n");
        }
        resourceWriter.write("\r\n");
      }
    } catch (final IOException e) {
      log.error("Failed to save print generation data general information", e);
    }
  }
}
