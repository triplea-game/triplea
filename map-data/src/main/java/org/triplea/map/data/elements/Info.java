package org.triplea.map.data.elements;

import lombok.Getter;
import org.triplea.map.reader.XmlReader;

@Getter
public class Info {
  public static final String TAG_NAME = "info";
  private final String name;
  private final String version;

  public Info(final XmlReader xmlReader) {
    name = xmlReader.getAttributeValue("name");
    version = xmlReader.getAttributeValue("version");
  }
}
