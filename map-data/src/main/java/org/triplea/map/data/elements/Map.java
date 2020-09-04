package org.triplea.map.data.elements;

import java.util.List;
import lombok.Getter;
import org.triplea.generic.xml.reader.Attribute;
import org.triplea.generic.xml.reader.TagList;

@Getter
public class Map {

  @TagList(Territory.class)
  private List<Territory> territories;

  @TagList(Connection.class)
  private List<Connection> connections;

  @Getter
  public static class Territory {
    @Attribute private String name;

    @Attribute(defaultValue = "false")
    private String water;
  }

  @Getter
  public static class Connection {
    @Attribute private String t1;
    @Attribute private String t2;
  }
}
