package org.triplea.map.reader;

import java.io.InputStream;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import lombok.experimental.UtilityClass;
import org.triplea.generic.xml.reader.XmlMapper;
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
      return new XmlMapper(streamReader).mapXmlToClass(Game.class);
    } finally {
      streamReader.close();
    }
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
