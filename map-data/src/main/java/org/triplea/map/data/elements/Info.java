package org.triplea.map.data.elements;

import lombok.Getter;
import org.triplea.generic.xml.reader.Attribute;

@Getter
public class Info {
  @Attribute private String name;
  @Attribute private String version;
}
