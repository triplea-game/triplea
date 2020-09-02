package org.triplea.map.reader;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
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
    return new XmlParser(tagName);
  }

  public AttributeParser addAttributeHandler(
      final String attributeName, final Consumer<String> attributeHandler) {
    return new AttributeParser(tagName, new HashMap<>())
        .addAttributeHandler(attributeName, attributeHandler);
  }

  public TagParser addChildTagHandler(final String childTagName, final ThrowingRunnable handler) {
    return new TagParser(tagName, new HashMap<>(), new HashMap<>())
        .addChildTagHandler(childTagName, handler);
  }

  public BodyParser addBodyHandler(final Consumer<String> bodyHandler) {
    return new BodyParser(tagName, Map.of(), bodyHandler);
  }

  @AllArgsConstructor(access = AccessLevel.PRIVATE)
  public static class AttributeParser {
    private final String currentTag;
    private final Map<String, Consumer<String>> attributeHandlers;

    public AttributeParser addAttributeHandler(
        final String attributeName, final Consumer<String> attributeHandler) {
      attributeHandlers.put(attributeName, attributeHandler);
      return this;
    }

    public TagParser addChildTagHandler(final String childTagName, final ThrowingRunnable tagHandler) {
      return new TagParser(currentTag, attributeHandlers, new HashMap<>())
          .addChildTagHandler(childTagName, tagHandler);
    }

    public BodyParser addBodyHandler(final Consumer<String> bodyHandler) {
      return new BodyParser(currentTag, attributeHandlers, bodyHandler);
    }

    public void parse(final XMLStreamReader xmlStreamReader) throws XMLStreamException {
      new GenericParser(currentTag, attributeHandlers, Map.of(), body -> {}).parse(xmlStreamReader);
    }
  }

  @AllArgsConstructor(access = AccessLevel.PRIVATE)
  public static class TagParser {
    private final String currentTag;
    private final Map<String, Consumer<String>> attributeHandlers;
    private final Map<String, ThrowingRunnable> childTagHandlers;

    public TagParser addChildTagHandler(final String childTagNaem, final ThrowingRunnable tagHandler) {
      childTagHandlers.put(childTagNaem, tagHandler);
      return this;
    }

    public void parse(final XMLStreamReader streamReader) throws XMLStreamException {
      new GenericParser(currentTag, attributeHandlers, childTagHandlers, body -> {})
          .parse(streamReader);
    }
  }

  @AllArgsConstructor(access = AccessLevel.PRIVATE)
  public static class BodyParser {
    private final String currentTag;
    private final Map<String, Consumer<String>> attributeHandlers;
    private Consumer<String> bodyHandler;

    public void parse(final XMLStreamReader streamReader) throws XMLStreamException {
      new GenericParser(currentTag, attributeHandlers, Map.of(), bodyHandler).parse(streamReader);
    }
  }

  @Log
  @AllArgsConstructor(access = AccessLevel.PRIVATE)
  public static class GenericParser {
    private final String currentTag;
    private final Map<String, Consumer<String>> attributeHandlers;
    private final Map<String, ThrowingRunnable> childTagHandlers;
    private Consumer<String> bodyHandler;

    public void parse(final XMLStreamReader streamReader) throws XMLStreamException {
      boolean endTagReached = false;

      log.info("Processing current tag: " + currentTag);

      if (streamReader.getEventType() == XMLStreamReader.START_ELEMENT) {
        for (int i = 0, n = streamReader.getAttributeCount(); i < n; i++) {
          final int index = i;

          final String attributeName = streamReader.getAttributeLocalName(index);
          final Consumer<String> attributeHandler = attributeHandlers.get(attributeName);
          if (attributeHandler != null) {
            String attributeValue = streamReader.getAttributeValue(index);
            attributeHandler.accept(attributeValue);
          }
        }
      }

      while (streamReader.hasNext() && !endTagReached) {
        log.info("event type: " + streamReader.getEventType());
        final int event = streamReader.next();
        switch (event) {
          case XMLStreamReader.START_ELEMENT:
            final String childTag = streamReader.getLocalName();
            log.info("start tag reached: " + childTag);
            final ThrowingRunnable childTagHandler = childTagHandlers.get(childTag);
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
            String endTagName = streamReader.getLocalName();
            log.info("end tag reached: " + endTagName);
            if (endTagName.equals(currentTag)) {
              endTagReached = true;
            }
            break;
        }
      }
    }
  }
}
