package org.triplea.map.data.elements;

import lombok.Getter;
import org.triplea.generic.xml.reader.annotations.Attribute;

@Getter
public class DiceSides {
  @Attribute(defaultInt = 6)
  private int value;
}
