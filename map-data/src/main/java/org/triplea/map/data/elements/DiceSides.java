package org.triplea.map.data.elements;

import lombok.Getter;
import org.triplea.map.reader.XmlReader;

@Getter
public class DiceSides {
  public static final String TAG_NAME = "diceSides";
  private String value;

  DiceSides(final XmlReader xmlReader) {
    value = xmlReader.getAttributeValue("value");
  }
}
