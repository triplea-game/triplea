package org.triplea.generic.xml.reader;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import javax.xml.stream.XMLStreamReader;
import org.triplea.java.function.ThrowingRunnable;

class XmlParser {
  private final String tagName;
  private final Map<String, ThrowingRunnable<?>> childTagHandlers = new HashMap<>();
  private Consumer<String> bodyHandler;

  XmlParser(final String tagName) {
    this.tagName = tagName.toUpperCase();
  }

  void childTagHandler(final String childTagName, final ThrowingRunnable<?> tagHandler) {
    childTagHandlers.put(childTagName.toUpperCase(), tagHandler);
  }

  void bodyHandler(final Consumer<String> bodyHandler) {
    this.bodyHandler = bodyHandler;
  }

  void parse(final XMLStreamReader streamReader) throws Throwable {
    StringBuilder textElementBuilder = new StringBuilder();
    while (streamReader.hasNext()) {
      final int event = streamReader.next();
      switch (event) {
        case XMLStreamReader.START_ELEMENT:
          final String childTag = streamReader.getLocalName().toUpperCase();
          final ThrowingRunnable<?> childTagHandler = childTagHandlers.get(childTag);
          if (childTagHandler != null) {
            childTagHandler.run();
          }
          break;
        case XMLStreamReader.CHARACTERS:
          if (streamReader.hasText() && bodyHandler != null) {
            final String text = streamReader.getText();
            textElementBuilder.append(text);
          }
          break;
        case XMLStreamReader.END_ELEMENT:
          if (bodyHandler != null) {
            bodyHandler.accept(textElementBuilder.toString().trim());
          }
          textElementBuilder = new StringBuilder();

          final String endTagName = streamReader.getLocalName();
          if (endTagName.equalsIgnoreCase(tagName)) {
            return;
          }
          break;
        default:
          break;
      }
    }
  }
}
