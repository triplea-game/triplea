package org.triplea.map.data.elements;

import java.util.List;
import lombok.Getter;
import org.triplea.generic.xml.reader.annotations.Attribute;
import org.triplea.generic.xml.reader.annotations.Tag;
import org.triplea.generic.xml.reader.annotations.TagList;

@Getter
public class Technology {

  @Tag private Technologies technologies;

  @TagList private List<PlayerTech> playerTechs;

  @Getter
  public static class Technologies {
    @TagList private List<TechName> techNames;

    @Getter
    public static class TechName {
      @Attribute String name;

      @Attribute(defaultValue = "")
      String tech;
    }
  }

  @Getter
  public static class PlayerTech {
    @Attribute private String player;

    @TagList private List<Category> categories;

    @Getter
    public static class Category {
      @Attribute private String name;

      @TagList private List<Tech> techs;

      @Getter
      public static class Tech {
        @Attribute private String name;
      }
    }
  }
}
