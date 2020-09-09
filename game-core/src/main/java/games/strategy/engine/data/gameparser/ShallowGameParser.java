package games.strategy.engine.data.gameparser;

import static com.google.common.base.Preconditions.checkNotNull;

import games.strategy.engine.data.GameParseException;
import java.io.InputStream;
import java.util.Optional;
import java.util.logging.Level;
import javax.xml.stream.XMLStreamException;
import lombok.experimental.UtilityClass;
import lombok.extern.java.Log;
import org.triplea.generic.xml.reader.XmlMapper;
import org.triplea.generic.xml.reader.exceptions.XmlParsingException;
import org.triplea.generic.xml.scanner.AttributeScannerParameters;
import org.triplea.generic.xml.scanner.XmlScanner;
import org.triplea.map.data.elements.ShallowParsedGame;

@UtilityClass
@Log
public class ShallowGameParser {

  public static Optional<String> readGameName(final String mapName, final InputStream stream) {
    checkNotNull(mapName);
    checkNotNull(stream);

    try {
      return Optional.of(
          new XmlScanner(stream)
              .scanForAttributeValue(
                  AttributeScannerParameters.builder().attributeName("name").tag("info").build())
              .orElseThrow(
                  () ->
                      new GameParseException(
                          "Error reading Game XML file in map: "
                              + mapName
                              + ". Game file is missing the tag: '<info name=\"Game Name\"/>'")));
    } catch (final GameParseException e) {
      log.log(
          Level.WARNING,
          "Error reading XMl file (invalid XML): " + mapName + ", " + e.getMessage(),
          e);
      return Optional.empty();

    } catch (final XMLStreamException e) {
      log.log(
          Level.WARNING,
          "Unexpected error reading Game XML file in map: " + mapName + ", " + e.getMessage(),
          e);
      return Optional.empty();
    }
  }

  public Optional<ShallowParsedGame> parseShallow(final InputStream inputStream) {
    try {
      return Optional.of(new XmlMapper(inputStream).mapXmlToObject(ShallowParsedGame.class));
    } catch (final XmlParsingException e) {
      log.log(Level.WARNING, "Unexpected error reading Game XML, " + e.getMessage(), e);
      return Optional.empty();
    }
  }
}
