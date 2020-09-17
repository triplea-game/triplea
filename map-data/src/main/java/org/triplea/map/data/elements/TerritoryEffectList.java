package org.triplea.map.data.elements;

import java.util.List;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.triplea.generic.xml.reader.annotations.Attribute;
import org.triplea.generic.xml.reader.annotations.TagList;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TerritoryEffectList {

  @XmlElement(name = "territoryEffect")
  @TagList
  private List<TerritoryEffect> territoryEffects;

  @Getter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class TerritoryEffect {
    @XmlAttribute @Attribute private String name;
  }
}
