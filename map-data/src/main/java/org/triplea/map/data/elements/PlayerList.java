package org.triplea.map.data.elements;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import java.util.List;
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
public class PlayerList {

  @XmlElement(name = "player")
  @TagList
  private List<Player> players;

  @XmlElement(name = "alliance")
  @TagList
  private List<Alliance> alliances;

  @Getter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Player {
    @XmlAttribute @Attribute private String name;

    @XmlAttribute @Attribute private Boolean optional;

    @XmlAttribute @Attribute private Boolean canBeDisabled;

    @XmlAttribute @Attribute private String defaultType;

    @XmlAttribute @Attribute private Boolean isHidden;
  }

  @Getter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Alliance {
    @XmlAttribute @Attribute private String player;
    @XmlAttribute @Attribute private String alliance;
  }
}
