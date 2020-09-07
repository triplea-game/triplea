package org.triplea.map.data.elements;

import java.util.List;
import lombok.Getter;
import org.triplea.generic.xml.reader.annotations.Attribute;
import org.triplea.generic.xml.reader.annotations.TagList;

@Getter
public class UnitList {
  @TagList private List<Unit> units;

  @Getter
  public static class Unit {
    @Attribute private String name;
  }
}
