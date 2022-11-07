package org.triplea.map.data.elements;

import java.io.IOException;
import java.io.InputStream;
import lombok.experimental.UtilityClass;
import org.triplea.generic.xml.reader.XmlMapper;
import org.triplea.generic.xml.reader.exceptions.XmlParsingException;

@UtilityClass
class XmlReaderTestUtils {

  static Game parseMapXml(final String xmlFileName) {
    try (InputStream stream = openFile(xmlFileName);
        XmlMapper xmlMapper = new XmlMapper(stream)) {
      return xmlMapper.mapXmlToObject(Game.class);
    } catch (final IOException | XmlParsingException e) {
      throw new AssertionError("Unexpected exception thrown", e);
    }
  }

  private static InputStream openFile(final String fileName) {
    return InfoTest.class.getClassLoader().getResourceAsStream(fileName);
  }
}
