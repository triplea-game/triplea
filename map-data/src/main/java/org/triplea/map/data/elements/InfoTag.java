package org.triplea.map.data.elements;

import javax.xml.stream.XMLStreamReader;
import lombok.Getter;

@Getter
public class InfoTag {
  public static final String TAG_NAME = "info";
  private final String name;
  private final String version;

  public InfoTag(final XMLStreamReader streamReader) {
    this.name = streamReader.getAttributeValue(null, "name");
    this.version = streamReader.getAttributeValue(null, "version");
  }
}
