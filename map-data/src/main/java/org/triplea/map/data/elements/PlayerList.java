package org.triplea.map.data.elements;

import java.util.List;
import lombok.Getter;
import org.triplea.generic.xml.reader.Attribute;
import org.triplea.generic.xml.reader.TagList;

@Getter
public class PlayerList {

  @TagList(Player.class)
  private List<Player> players;

  @TagList(Alliance.class)
  private List<Alliance> alliances;

  @Getter
  public static class Player {
    @Attribute private String name;

    @Attribute(defaultValue = "false")
    private String optional;

    @Attribute(defaultValue = "false")
    private String canBeDisabled;

    @Attribute(defaultValue = "Human")
    private String defaultType;

    @Attribute(defaultValue = "false")
    private String isHidden;
  }

  @Getter
  public static class Alliance {
    @Attribute private String player;
    @Attribute private String alliance;
  }
}
