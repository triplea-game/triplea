package org.triplea.map.data.elements;

import lombok.Getter;
import org.triplea.map.reader.generic.xml.Attribute;

@Getter
public class Info {
  @Attribute private String name;
  @Attribute private String version;
}
