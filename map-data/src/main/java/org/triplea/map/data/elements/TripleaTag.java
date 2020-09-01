package org.triplea.map.data.elements;

import javax.xml.stream.XMLStreamReader;
import lombok.Getter;

@Getter
public class TripleaTag {
  public static final String TAG_NAME = "triplea";
  private final String minimumVersion;

  public TripleaTag(final XMLStreamReader streamReader) {
    this.minimumVersion = streamReader.getAttributeValue(null, "minimumVersion");
  }
}
