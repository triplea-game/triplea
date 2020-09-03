package org.triplea.map.reader.generic.xml;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Level;
import javax.annotation.Nonnull;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.triplea.java.function.ThrowingRunnable;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Log
public class XmlParser {
  @Nonnull private final String tagName;

  public static XmlParser tag(final String tagName) {
    return new XmlParser(tagName.toUpperCase());
  }

  public TagParser childTagHandler(final String childTagName, final ThrowingRunnable handler) {
    return new TagParser(tagName, new HashMap<>()).childTagHandler(childTagName, handler);
  }

  public BodyParser bodyHandler(final Consumer<String> bodyHandler) {
    return new BodyParser(tagName, bodyHandler);
  }

  @AllArgsConstructor(access = AccessLevel.PRIVATE)
  public static class TagParser {
    private final String currentTag;
    private final Map<String, ThrowingRunnable> childTagHandlers;

    public TagParser(String currentTag) {
      this.currentTag = currentTag.toUpperCase();
      this.childTagHandlers = new HashMap<>();
    }

    public TagParser childTagHandler(final String childTagName, final ThrowingRunnable tagHandler) {
      childTagHandlers.put(childTagName.toUpperCase(), tagHandler);
      return this;
    }

    public void parse(final XMLStreamReader xmlReader) throws XMLStreamException {
      new GenericParser(currentTag, childTagHandlers, body -> {}).parse(xmlReader);
    }
  }

  @AllArgsConstructor(access = AccessLevel.PRIVATE)
  public static class BodyParser {
    private final String currentTag;
    private Consumer<String> bodyHandler;

    public void parse(final XMLStreamReader xmlReader) throws XMLStreamException {
      new GenericParser(currentTag, Map.of(), bodyHandler).parse(xmlReader);
    }
  }

  @Log
  @AllArgsConstructor(access = AccessLevel.PRIVATE)
  public static class GenericParser {
    private final String currentTag;
    private final Map<String, ThrowingRunnable> childTagHandlers;
    private Consumer<String> bodyHandler;

    public void parse(final XMLStreamReader streamReader) throws XMLStreamException {
      boolean endTagReached = false;

      if (log.isLoggable(Level.FINE)) {
        log.fine("Processing tag: " + currentTag);
      }

      while (streamReader.hasNext() && !endTagReached) {
        log.info("event type: " + streamReader.getEventType());
        final int event = streamReader.next();
        switch (event) {
          case XMLStreamReader.START_ELEMENT:
            final String childTag = streamReader.getLocalName();
            log.info("start tag reached: " + childTag);
            final ThrowingRunnable childTagHandler = childTagHandlers.get(childTag.toUpperCase());
            if (childTagHandler != null) {
              try {
                childTagHandler.run();
              } catch (final Throwable throwable) {
                throw new XMLStreamException("Parsing failed on tag: " + currentTag, throwable);
              }
            }
            break;
          case XMLStreamReader.CHARACTERS:
            if (streamReader.hasText()) {
              final String text = streamReader.getText();
              log.info("Characters Event reached: " + text);
              bodyHandler.accept(text);
            }
            break;
          case XMLStreamReader.END_ELEMENT:
            final String endTagName = streamReader.getLocalName();
            log.info("end tag reached: " + endTagName);
            if (endTagName.equalsIgnoreCase(currentTag)) {
              endTagReached = true;
            }
            break;
        }
      }
    }
  }
}
