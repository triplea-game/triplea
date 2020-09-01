package org.triplea.map.reader;

import java.io.InputStream;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import lombok.experimental.UtilityClass;
import org.triplea.map.data.ParsedMap;
import org.triplea.map.data.elements.AttachmentListTag;
import org.triplea.map.data.elements.InfoTag;
import org.triplea.map.data.elements.TripleaTag;

@UtilityClass
public class MapElementReader {

  public static MapReadResult readXml(final String xmlFile, final InputStream inputStream) {
    try {
      return readXmlThrowing(inputStream);
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

  private static MapReadResult readXmlThrowing(final InputStream inputStream)
      throws XMLStreamException {

    final XMLInputFactory inputFactory = XMLInputFactory.newInstance();
    final XMLStreamReader streamReader = inputFactory.createXMLStreamReader(inputStream);

    final ParsedMap.Builder mapBuilder = ParsedMap.builder();

    try {
      while (streamReader.hasNext()) {
        final int eventType = streamReader.next();
        doRead(eventType, streamReader, mapBuilder);
      }
    } finally {
      streamReader.close();
    }

    return MapReadResult.builder().parsedMap(mapBuilder.build()).build();
  }

  private static void doRead(
      final int eventType, final XMLStreamReader streamReader, final ParsedMap.Builder mapBuilder)
      throws XMLStreamException {

    switch (eventType) {
      case XMLStreamReader.START_ELEMENT:
        final String elementName = streamReader.getLocalName();

        switch (elementName) {
          case InfoTag.TAG_NAME:
            mapBuilder.infoTag(new InfoTag(streamReader));
            break;
          case TripleaTag.TAG_NAME:
            mapBuilder.tripleaTag(new TripleaTag(streamReader));
            break;
          case AttachmentListTag.TAG_NAME:
            mapBuilder.attachmentListTag(new AttachmentListTag(streamReader));
            break;
        }
        break;
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
