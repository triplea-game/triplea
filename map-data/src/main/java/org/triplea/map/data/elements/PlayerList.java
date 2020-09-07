package org.triplea.map.data.elements;

import java.util.List;
import lombok.Getter;
import org.triplea.generic.xml.reader.annotations.Attribute;
import org.triplea.generic.xml.reader.annotations.TagList;

@Getter
public class PlayerList {

  @TagList private List<Player> players;

  @TagList private List<Alliance> alliances;

  @Getter
  public static class Player {
    @Attribute private String name;

    @Attribute private boolean optional;

    @Attribute private boolean canBeDisabled;

    @Attribute(defaultValue = "Human")
    private String defaultType;

    @Attribute private boolean isHidden;
  }

  @Getter
  public static class Alliance {
    @Attribute private String player;
    @Attribute private String alliance;
  }
}
