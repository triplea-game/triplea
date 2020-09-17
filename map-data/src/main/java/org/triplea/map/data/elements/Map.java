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
public class Map {

  @XmlElement(name = "territory")
  @TagList
  private List<Territory> territories;

  @XmlElement(name = "connection")
  @TagList
  private List<Connection> connections;

  @Getter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Territory {
    @XmlAttribute @Attribute private String name;

    @XmlAttribute @Attribute private Boolean water;
  }

  @Getter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Connection {
    @XmlAttribute @Attribute private String t1;
    @XmlAttribute @Attribute private String t2;
  }
}
