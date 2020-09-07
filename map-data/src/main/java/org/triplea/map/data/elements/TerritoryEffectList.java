package org.triplea.map.data.elements;

import java.util.List;
import lombok.Getter;
import org.triplea.generic.xml.reader.annotations.Attribute;
import org.triplea.generic.xml.reader.annotations.TagList;

@Getter
public class TerritoryEffectList {

  @TagList private List<TerritoryEffect> territoryEffects;

  @Getter
  public static class TerritoryEffect {
    @Attribute private String name;
  }
}
