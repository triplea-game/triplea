package org.triplea.generic.xml.reader;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Level;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import lombok.extern.java.Log;
import org.triplea.java.function.ThrowingRunnable;

@Log
class XmlParser {
  private final String tagName;
  private final Map<String, ThrowingRunnable> childTagHandlers = new HashMap<>();
  private Consumer<String> bodyHandler;

  XmlParser(final String tagName) {
    this.tagName = tagName.toUpperCase();
  }

  public void childTagHandler(final String childTagName, final ThrowingRunnable tagHandler) {
    childTagHandlers.put(childTagName.toUpperCase(), tagHandler);
  }

  public void bodyHandler(final Consumer<String> bodyHandler) {
    this.bodyHandler = bodyHandler;
  }

  public void parse(final XMLStreamReader streamReader) throws XMLStreamException {
    if (log.isLoggable(Level.FINE)) {
      log.fine("Processing tag: " + tagName);
    }

    StringBuilder textElementBuilder = new StringBuilder();
    while (streamReader.hasNext()) {
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
              throw new XMLStreamException("Parsing failed on tag: " + tagName, throwable);
            }
          }
          break;
        case XMLStreamReader.CHARACTERS:
          if (streamReader.hasText() && bodyHandler != null) {
            final String text = streamReader.getText();
            log.info("Characters Event reached: " + text);
            textElementBuilder.append(text);
          }
          break;
        case XMLStreamReader.END_ELEMENT:
          if (bodyHandler != null) {
            bodyHandler.accept(textElementBuilder.toString().trim());
          }
          textElementBuilder = new StringBuilder();

          final String endTagName = streamReader.getLocalName();
          log.info("end tag reached: " + endTagName);
          if (endTagName.equalsIgnoreCase(tagName)) {
            return;
          }
          break;
      }
    }
  }
}
