package games.strategy.engine.data.gameparser;

import games.strategy.engine.data.GameData;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TripleAMapValidator {
  /**
   * Uses a logback.xml to output logs to STDOUT. TODO: Remove the logback.xml when main method is
   * removed.
   *
   * @param args paths to game XML files
   */
  public static void main(String[] args) {
    if (args.length == 0) {
      log.warn("Usage: TripleAMapValidator <path-to-game1.xml> (<path-to-game2.xml>) ...");
      System.exit(1);
    }
    for (final String arg : args) {
      try {
        final Path xmlFilePath = Path.of(arg);
        Optional<GameData> optionalGameData = GameParser.parse(xmlFilePath, false);
        if (optionalGameData.isPresent()) {
          log.info("✅ Map is valid {}", arg);
        } else {
          log.error("❌ Validation failed for {} ", arg);
          System.exit(1);
        }
      } catch (InvalidPathException e) {
        log.error("❌ Validation failed for {} ", arg, e);
        System.exit(1);
      }
    }
  }
}
