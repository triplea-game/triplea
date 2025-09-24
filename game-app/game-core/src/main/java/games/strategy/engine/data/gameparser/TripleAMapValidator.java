package games.strategy.engine.data.gameparser;

import games.strategy.engine.data.GameData;
import java.nio.file.Path;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TripleAMapValidator {
  public static void main(String[] args) throws Exception {
    if (args.length == 0) {
      log.warn("Usage: TripleAMapValidator <path-to-game1.xml> (<path-to-game2.xml>) ...");
      return;
    }
    for (final String arg : args) {
      try {
        final Path xmlFilePath = Path.of(arg);
        Optional<GameData> optionalGameData = GameParser.parse(xmlFilePath, false);
        if (optionalGameData.isPresent()) {
          log.info("✅ Map is valid {}", arg);
        } else {
          log.error("❌ Validation failed for {} ", arg);
          System.exit(2);
        }
      } catch (Exception e) {
        log.error("❌ Validation failed for {} ", arg, e);
        System.exit(2);
      }
    }
  }
}
