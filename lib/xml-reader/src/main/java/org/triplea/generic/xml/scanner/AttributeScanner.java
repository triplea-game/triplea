package org.triplea.generic.xml.scanner;

import java.util.Locale;
import java.util.Optional;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import lombok.experimental.UtilityClass;

/**
 * Searches for a given XML tag and attribute name and returns the attribute value. Designed to run
 * quickly and only partially scan XML content.
 */
@UtilityClass
class AttributeScanner {
  Optional<String> scanForAttributeValue(
      final XMLStreamReader streamReader, final AttributeScannerParameters parameters)
      throws XMLStreamException {

    while (streamReader.hasNext()) {
      final int event = streamReader.next();
      if (event == XMLStreamReader.START_ELEMENT) {
        final String tag = streamReader.getLocalName().toUpperCase(Locale.ENGLISH);
        if (tag.equalsIgnoreCase(parameters.getTag())) {

          final String attributeValue =
              streamReader.getAttributeValue(null, parameters.getAttributeName());
          if (attributeValue != null) {
            return Optional.of(attributeValue);
          }
        }
      }
    }
    return Optional.empty();
  }
}
