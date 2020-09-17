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
public class UnitList {
  @XmlElement(name = "unit")
  @TagList
  private List<Unit> units;

  @Getter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Unit {
    @XmlAttribute @Attribute private String name;
  }
}
