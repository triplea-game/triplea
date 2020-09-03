package org.triplea.map.data.elements;

import lombok.Getter;
import org.triplea.map.reader.XmlReader;

@Getter
public class Triplea {
  public static final String TAG_NAME = "triplea";
  private final String minimumVersion;

  public Triplea(final XmlReader xmlReader) {
    this.minimumVersion = xmlReader.getAttributeValue("minimumVersion");
  }
}
