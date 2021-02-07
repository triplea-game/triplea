package games.strategy.engine.data.gameparser;

import java.io.InputStream;
import java.util.Optional;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.triplea.generic.xml.reader.XmlMapper;
import org.triplea.generic.xml.reader.exceptions.XmlParsingException;
import org.triplea.map.data.elements.ShallowParsedGame;

@UtilityClass
@Slf4j
public class ShallowGameParser {
  public Optional<ShallowParsedGame> parseShallow(final InputStream inputStream) {
    try {
      return Optional.of(new XmlMapper(inputStream).mapXmlToObject(ShallowParsedGame.class));
    } catch (final XmlParsingException e) {
      log.warn("Unexpected error reading Game XML, " + e.getMessage(), e);
      return Optional.empty();
    }
  }
}
