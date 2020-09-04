package org.triplea.map.data.elements;

import java.util.List;
import lombok.Getter;
import org.triplea.generic.xml.reader.Attribute;
import org.triplea.generic.xml.reader.TagList;

@Getter
public class TerritoryEffectList {

  @TagList(TerritoryEffect.class)
  private List<TerritoryEffect> territoryEffects;

  @Getter
  public static class TerritoryEffect {
    @Attribute private String name;
  }
}
