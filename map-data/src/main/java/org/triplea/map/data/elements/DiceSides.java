package org.triplea.map.data.elements;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import lombok.Getter;
import org.triplea.map.reader.XmlParser;

@Getter
public class DiceSides {
  public static final String TAG_NAME = "diceSides";
  private String value;

  DiceSides(final XMLStreamReader streamReader) throws XMLStreamException {
    new XmlParser(TAG_NAME)
        .addAttributeHandler("value", attributeValue -> value = attributeValue)
        .parse(streamReader);
  }
}
