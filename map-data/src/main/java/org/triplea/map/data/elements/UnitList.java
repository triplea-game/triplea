package org.triplea.map.data.elements;

import java.util.List;
import lombok.Getter;
import org.triplea.generic.xml.reader.Attribute;
import org.triplea.generic.xml.reader.TagList;

@Getter
public class UnitList {
  @TagList(Unit.class)
  private List<Unit> units;

  @Getter
  public static class Unit {
    @Attribute private String name;
  }
}
