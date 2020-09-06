package games.strategy.engine.data.gameparser;

import static com.google.common.base.Preconditions.checkNotNull;

import games.strategy.engine.data.GameParseException;
import java.io.InputStream;
import javax.xml.stream.XMLStreamException;
import lombok.experimental.UtilityClass;
import org.triplea.generic.xml.scanner.AttributeScannerParameters;
import org.triplea.generic.xml.scanner.XmlScanner;

@UtilityClass
public class ShallowGameParser {

  public static String readGameName(final String mapName, final InputStream stream)
      throws GameParseException {
    checkNotNull(mapName);
    checkNotNull(stream);

    try {
      return new XmlScanner(stream)
          .scanForAttributeValue(
              AttributeScannerParameters.builder().attributeName("name").tag("info").build())
          .orElseThrow(
              () ->
                  new GameParseException(
                      "Error reading Game XML file in map: "
                          + mapName
                          + ". Game file is missing the tag: '<info name=\"Game Name\"/>'"));
    } catch (final XMLStreamException e) {
      throw new GameParseException(
          "Unexpected error reading Game XML file in map: " + mapName + ", " + e.getMessage(), e);
    }
  }
}
