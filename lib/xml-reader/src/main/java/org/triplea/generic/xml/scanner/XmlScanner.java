package org.triplea.generic.xml.scanner;

import java.io.InputStream;
import java.util.Optional;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import lombok.extern.slf4j.Slf4j;

/**
 * Scans XML for a specific tag and returns specific data. Does not read the full XML file. This is
 * meant to be a quick read of an XML document if only partial data is needed.
 */
@Slf4j
public class XmlScanner {
  private final XMLStreamReader xmlStreamReader;

  public XmlScanner(final InputStream inputStream) throws XMLStreamException {
    final XMLInputFactory inputFactory = XMLInputFactory.newInstance();
    xmlStreamReader = inputFactory.createXMLStreamReader(inputStream);
  }

  /**
   * Scans the XML looking for a specific tag with a specific attribute and returns that attribute
   * value. Once found the XML scanning halts.
   *
   * @return The attribute value found, or an empty optional if not found.
   */
  public Optional<String> scanForAttributeValue(final AttributeScannerParameters parameters)
      throws XMLStreamException {
    try {
      return AttributeScanner.scanForAttributeValue(xmlStreamReader, parameters);
    } finally {
      try {
        xmlStreamReader.close();
      } catch (final XMLStreamException e) {
        log.info("Failed to close XML stream", e);
      }
    }
  }
}
