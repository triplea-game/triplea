package org.triplea.map.data.elements;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import lombok.Getter;
import org.triplea.map.reader.XmlParser;

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
  private DiceSidesTag diceSidesTag;

  public GameTag(final XMLStreamReader streamReader) throws XMLStreamException {
    new XmlParser(TAG_NAME)
        .addChildTagHandler(InfoTag.TAG_NAME, () -> infoTag = new InfoTag(streamReader))
        .addChildTagHandler(TripleaTag.TAG_NAME, () -> tripleaTag = new TripleaTag(streamReader))
        .addChildTagHandler(
            AttachmentListTag.TAG_NAME,
            () -> attachmentListTag = new AttachmentListTag(streamReader))
        .addChildTagHandler(
            DiceSidesTag.TAG_NAME, () -> diceSidesTag = new DiceSidesTag(streamReader))
        .parse(streamReader);
  }
}
