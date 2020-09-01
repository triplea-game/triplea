package org.triplea.map.reader;

import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import javax.annotation.Nonnull;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import lombok.Builder;
import lombok.Singular;

@Builder
public class TagParser {
  @Nonnull private final String tagName;
  @Singular private final Map<String, Consumer<XMLStreamReader>> childTagHandlers;

  public void parseTag(final XMLStreamReader streamReader) throws XMLStreamException {
    boolean endTagReached = false;
    while (streamReader.hasNext() && !endTagReached) {
      final int event = streamReader.next();
      switch (event) {
        case XMLStreamReader.START_ELEMENT:
          final String childTag = streamReader.getLocalName();
          Optional.ofNullable(childTagHandlers.get(childTag))
              .ifPresent(childTagHandler -> childTagHandler.accept(streamReader));
          break;
        case XMLStreamReader.END_ELEMENT:
          if (streamReader.getLocalName().equals(tagName)) {
            endTagReached = true;
          }
          break;
      }
    }
  }
}
