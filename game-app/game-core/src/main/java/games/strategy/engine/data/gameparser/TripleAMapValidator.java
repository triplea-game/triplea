package games.strategy.engine.data.gameparser;

import java.nio.file.Path;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TripleAMapValidator {
  public static void main(String[] args) throws Exception {
    if (args.length == 0) {
      log.warn("Usage: TripleAMapValidator <path-to-game1.xml> (<path-to-game2.xml>) ...");
      System.exit(1);
    }
    for (final String arg : args) {
      try {
        final Path xmlFilePath = Path.of(arg);
        GameParser.parse(xmlFilePath, false);
        log.info("✅ Map is valid {}", arg);
      } catch (Exception e) {
        log.error("❌ Validation failed for {} ", arg, e);
        System.exit(2);
      }
    }
  }
}
