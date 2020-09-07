package org.triplea.map.data.elements;

import java.util.List;
import lombok.Getter;
import org.triplea.generic.xml.reader.annotations.Attribute;
import org.triplea.generic.xml.reader.annotations.TagList;

@Getter
public class Map {

  @TagList private List<Territory> territories;

  @TagList private List<Connection> connections;

  @Getter
  public static class Territory {
    @Attribute private String name;

    @Attribute private boolean water;
  }

  @Getter
  public static class Connection {
    @Attribute private String t1;
    @Attribute private String t2;
  }
}
