package org.triplea.map.data.elements;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import lombok.Getter;

/**
 * Represents all of the org.triplea.map.data read from a map. The org.triplea.map.data is in a
 * 'raw' form where we simply represent the org.triplea.map.data as closely as possible as POJOs
 * without semantic meaning.
 */
@Getter
public class GameTag {
  public static final String TAG_NAME = "game";

  private InfoTag infoTag;
  private TripleaTag tripleaTag;
  private AttachmentListTag attachmentListTag;

  public GameTag(final XMLStreamReader streamReader) throws XMLStreamException {
    while(streamReader.hasNext()) {
      final int event = streamReader.next();
      switch (event) {
        case XMLStreamReader.START_ELEMENT:
          final String tagName = streamReader.getLocalName();
          switch (tagName) {
            case InfoTag.TAG_NAME:
              infoTag = new InfoTag(streamReader);
              break;
            case TripleaTag.TAG_NAME:
              tripleaTag = new TripleaTag(streamReader);
              break;
            case AttachmentListTag.TAG_NAME:
              attachmentListTag = new AttachmentListTag(streamReader);
              break;
          }
      }
    }
  }
}
