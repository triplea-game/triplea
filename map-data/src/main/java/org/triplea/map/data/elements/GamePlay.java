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
public class GamePlay {

  @XmlElement(name = "delegate")
  @TagList
  private List<Delegate> delegates;

  @XmlElement @Tag private Sequence sequence;
  @XmlElement @Tag private Offset offset;

  @Getter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Delegate {
    @XmlAttribute @Attribute private String name;
    @XmlAttribute @Attribute private String javaClass;
    @XmlAttribute @Attribute private String display;
  }

  @Getter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Sequence {
    @XmlElement(name = "step")
    @TagList
    private List<Step> steps;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Step {
      @XmlAttribute @Attribute private String name;
      @XmlAttribute @Attribute private String delegate;
      @XmlAttribute @Attribute private String player;
      @XmlAttribute @Attribute private Integer maxRunCount;
      @XmlAttribute @Attribute private String display;

      @XmlElement(name = "stepProperty")
      @TagList
      private List<StepProperty> stepProperties;

      @Getter
      @Builder
      @NoArgsConstructor
      @AllArgsConstructor
      public static class StepProperty {
        @XmlAttribute @Attribute private String name;
        @XmlAttribute @Attribute private String value;
      }
    }
  }

  @Getter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Offset {
    @XmlAttribute @Attribute private Integer round;
  }
}
