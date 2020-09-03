package org.triplea.map.data.elements;

import javax.xml.stream.XMLStreamException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.triplea.map.reader.XmlParser;
import org.triplea.map.reader.XmlReader;

/**
 * Represents all of the org.triplea.map.data read from a map. The org.triplea.map.data is in a
 * 'raw' form where we simply represent the org.triplea.map.data as closely as possible as POJOs
 * without semantic meaning.
 */
@Getter
@Builder
@EqualsAndHashCode
@AllArgsConstructor
public class Game {
  public static final String TAG_NAME = "game";

  private Info info;
  private Triplea triplea;
  private AttachmentList attachmentList;
  private DiceSides diceSides;
  private GamePlay gamePlay;
  private Initialize initialize;

  public Game(final XmlReader xmlReader) throws XMLStreamException {
    XmlParser.tag(TAG_NAME)
        .childTagHandler(Info.TAG_NAME, () -> info = new Info(xmlReader))
        .childTagHandler(Triplea.TAG_NAME, () -> triplea = new Triplea(xmlReader))
        .childTagHandler(
            AttachmentList.TAG_NAME, () -> attachmentList = new AttachmentList(xmlReader))
        .childTagHandler(DiceSides.TAG_NAME, () -> diceSides = new DiceSides(xmlReader))
        .childTagHandler(GamePlay.TAG_NAME, () -> gamePlay = new GamePlay(xmlReader))
        .childTagHandler(Initialize.TAG_NAME, () -> initialize = new Initialize(xmlReader))
        .parse(xmlReader);
  }
}
