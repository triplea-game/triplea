package org.triplea.map.data.elements;

import java.util.List;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.triplea.generic.xml.reader.annotations.Attribute;
import org.triplea.generic.xml.reader.annotations.Tag;
import org.triplea.generic.xml.reader.annotations.TagList;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Technology {

  @XmlElement @Tag private Technologies technologies;

  @XmlElement(name = "playerTech")
  @TagList
  private List<PlayerTech> playerTechs;

  @Getter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Technologies {
    @XmlElement(name = "techname")
    @TagList
    private List<TechName> techNames;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TechName {
      @XmlAttribute @Attribute String name;
      @XmlAttribute @Attribute String tech;
    }
  }

  @Getter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class PlayerTech {
    @XmlAttribute @Attribute private String player;

    @XmlElement(name = "category")
    @TagList
    private List<Category> categories;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Category {
      @XmlAttribute @Attribute private String name;

      @XmlElement(name = "tech")
      @TagList
      private List<Tech> techs;

      @Getter
      @Builder
      @NoArgsConstructor
      @AllArgsConstructor
      public static class Tech {
        @XmlAttribute @Attribute private String name;
      }
    }
  }
}
