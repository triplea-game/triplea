package games.strategy.engine.data.gameparser;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.InputStream;
import java.util.Optional;
import javax.xml.stream.XMLStreamException;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.triplea.generic.xml.reader.XmlMapper;
import org.triplea.generic.xml.reader.exceptions.XmlParsingException;
import org.triplea.generic.xml.scanner.AttributeScannerParameters;
import org.triplea.generic.xml.scanner.XmlScanner;
import org.triplea.map.data.elements.ShallowParsedGame;

@UtilityClass
@Slf4j
public class ShallowGameParser {

  /**
   * Scans an XML file looking for the 'game name attribute and returns it if found.
   *
   * @param xmlFileName The name of the XML file, used for error reporting.
   * @param stream An input stream to the XML file.
   */
  public static Optional<String> readGameName(final String xmlFileName, final InputStream stream) {
    checkNotNull(xmlFileName);
    checkNotNull(stream);

    try {
      return Optional.of(
          new XmlScanner(stream)
              .scanForAttributeValue(
                  AttributeScannerParameters.builder().attributeName("name").tag("info").build())
              .orElseThrow(
                  () ->
                      new GameParseException(
                          "Error reading XML file: "
                              + xmlFileName
                              + ". Game file is missing the tag: '<info name=\"Game Name\"/>'")));
    } catch (final GameParseException e) {
      log.warn("Error reading XMl file (invalid XML): " + xmlFileName + ", " + e.getMessage(), e);
      return Optional.empty();
    } catch (final XMLStreamException e) {
      log.warn("Unexpected error reading XML file: " + xmlFileName + ", " + e.getMessage(), e);
      return Optional.empty();
    }
  }

  public Optional<ShallowParsedGame> parseShallow(final InputStream inputStream) {
    try {
      return Optional.of(new XmlMapper(inputStream).mapXmlToObject(ShallowParsedGame.class));
    } catch (final XmlParsingException e) {
      log.warn("Unexpected error reading Game XML, " + e.getMessage(), e);
      return Optional.empty();
    }
  }
}
