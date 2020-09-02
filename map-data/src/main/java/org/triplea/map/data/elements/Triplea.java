package org.triplea.map.data.elements;

import javax.xml.stream.XMLStreamReader;
import lombok.Getter;

@Getter
public class Triplea {
  public static final String TAG_NAME = "triplea";
  private final String minimumVersion;

  public Triplea(final XMLStreamReader streamReader) {
    this.minimumVersion = streamReader.getAttributeValue(null, "minimumVersion");
  }
}
