package org.triplea.generic.xml.scanner;

import java.util.Optional;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import lombok.experimental.UtilityClass;

/**
 * Searches for body tag content in a XML file (originally built to read 'game-notes'). This
 * searches for a specific tag, with attribute key value pairs, then looks for a specific child tag
 * underneath that and returns the tag body content.
 */
@UtilityClass
class BodyTextScanner {

  /*
   Implementation notes:

   This works by keeping track of state of whether we have found the parent tag and the child tag.
   When we first see the parent tag with matching attribute name and value then we toggle
   'foundParent' to true. When we find the child tag we'll toggle "foundChild" to true and
   from there we'll buffer any body content that we find.

   When we hit the end tag for the child we'll return the buffered content. If we hit
   an end tag and "foundChild" is false, then the parent tag did not contain the desired
   child tag and we flag "foundParent" to false and we keep searching for another parent
   tag that matches, and then we'll search for a child tag and so on until we find something
   or return empty if we never do find a match.

  */
  Optional<String> scanForBodyText(
      final XMLStreamReader streamReader, final BodyTextScannerParameters parameters)
      throws XMLStreamException {

    boolean foundParent = false;
    boolean foundChild = false;
    final StringBuilder bodyText = new StringBuilder();

    while (streamReader.hasNext()) {
      final int event = streamReader.next();
      switch (event) {
        case XMLStreamReader.START_ELEMENT:
          final String tagName = streamReader.getLocalName().toUpperCase();
          if (!foundParent && parentTagMatchesParameters(tagName, streamReader, parameters)) {
            foundParent = true;
          } else if (foundParent && tagName.equalsIgnoreCase(parameters.getChildTag())) {
            foundChild = true;
          }
          break;
        case XMLStreamReader.CHARACTERS:
          if (foundChild) {
            bodyText.append(streamReader.getText());
          }
          break;
        case XMLStreamReader.END_ELEMENT:
          final String endTagName = streamReader.getLocalName();
          if (foundChild && endTagName.equalsIgnoreCase(parameters.getChildTag())) {
            // child end tag reached, return the body content we read!
            return Optional.of(bodyText.toString().trim());
          } else if (endTagName.equalsIgnoreCase(parameters.getParentTag())) {
            // parent end tag reached but we never matched the child, keep searching
            foundParent = false;
          }
          break;
        default:
          break;
      }
    }
    return Optional.empty();
  }

  private boolean parentTagMatchesParameters(
      final String tagName,
      final XMLStreamReader streamReader,
      final BodyTextScannerParameters parameters) {

    return tagName.equalsIgnoreCase(parameters.getParentTag())
        && parameters
            .getParentTagAttributeValue()
            .equalsIgnoreCase(
                streamReader.getAttributeValue(null, parameters.getParentTagAttributeName()));
  }
}
