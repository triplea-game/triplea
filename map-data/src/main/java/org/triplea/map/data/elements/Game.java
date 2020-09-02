package org.triplea.map.data.elements;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.triplea.map.reader.XmlParser;

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

  public Game(final XMLStreamReader streamReader) throws XMLStreamException {
    new XmlParser(TAG_NAME)
        .addChildTagHandler(Info.TAG_NAME, () -> info = new Info(streamReader))
        .addChildTagHandler(Triplea.TAG_NAME, () -> triplea = new Triplea(streamReader))
        .addChildTagHandler(
            AttachmentList.TAG_NAME,
            () -> attachmentList = new AttachmentList(streamReader))
        .addChildTagHandler(
            DiceSides.TAG_NAME, () -> diceSides = new DiceSides(streamReader))
        .addChildTagHandler(GamePlay.TAG_NAME, () -> gamePlay = new GamePlay(streamReader))
        .addChildTagHandler(Initialize.TAG_NAME, () -> initialize = new Initialize(streamReader))
        .parse(streamReader);
  }
}
