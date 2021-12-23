package org.triplea.map.xml.writer;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import java.nio.file.Path;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.triplea.map.data.elements.Game;

@Slf4j
@UtilityClass
public class GameXmlWriter {
  public void exportXml(final Game game, final Path toPath) {
    try {
      final JAXBContext context = JAXBContext.newInstance(Game.class);
      final Marshaller marshaller = context.createMarshaller();
      marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
      marshaller.marshal(game, toPath.toFile());
    } catch (final JAXBException e) {
      log.error("Error writing game data to XML: " + e.getMessage(), e);
    }
  }
}
