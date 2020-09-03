package org.triplea.map.reader;

import java.io.InputStream;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import lombok.experimental.UtilityClass;
import org.triplea.map.data.elements.Game;

@UtilityClass
public class MapElementReader {

  public static MapReadResult readXml(final String xmlFile, final InputStream inputStream) {
    try {
      final Game game = readXmlThrowing(inputStream);
      return MapReadResult.builder().game(game).build();
    } catch (final XMLStreamException e) {
      return MapReadResult.builder()
          .errorMessage(
              "Invalid XML, parse error reading: "
                  + xmlFile
                  + "\n"
                  + formatErrorMessage(new StringBuffer(), e))
          .build();
    }
  }

  private static Game readXmlThrowing(final InputStream inputStream) throws XMLStreamException {

    final XMLInputFactory inputFactory = XMLInputFactory.newInstance();
    final XMLStreamReader streamReader = inputFactory.createXMLStreamReader(inputStream);

    try {
      return doRead(streamReader);
    } finally {
      streamReader.close();
    }
  }

  private static Game doRead(final XMLStreamReader streamReader) throws XMLStreamException {

    final int eventType = streamReader.next();

    switch (eventType) {
      case XMLStreamReader.START_ELEMENT:
        final String elementName = streamReader.getLocalName();

        switch (elementName) {
          case Game.TAG_NAME:
            return new Game(new XmlReader(streamReader));
        }
    }
    throw new XMLStreamException("Did not find a 'game' tag as a top level and first tag");
  }

  /** Recursive method to concatenate to a string buffer all nested exception messages. */
  private static StringBuffer formatErrorMessage(
      final StringBuffer stringBuffer, final Throwable exception) {
    if (exception == null) {
      return stringBuffer;
    } else {
      stringBuffer.append(exception.getMessage()).append("\n");
      return formatErrorMessage(stringBuffer, exception.getCause());
    }
  }
}
