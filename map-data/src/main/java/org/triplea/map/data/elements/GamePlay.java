package org.triplea.map.data.elements;

import java.util.List;
import lombok.Getter;
import org.triplea.generic.xml.reader.annotations.Attribute;
import org.triplea.generic.xml.reader.annotations.Tag;
import org.triplea.generic.xml.reader.annotations.TagList;

@Getter
public class GamePlay {

  @TagList private List<Delegate> delegates;

  @Tag private Sequence sequence;
  @Tag private Offset offset;

  @Getter
  public static class Delegate {
    @Attribute private String name;
    @Attribute private String javaClass;
    @Attribute private String display;
  }

  @Getter
  public static class Sequence {
    @TagList private List<Step> steps;

    @Getter
    public static class Step {
      @Attribute private String name;
      @Attribute private String delegate;
      @Attribute private String player;
      @Attribute private int maxRunCount;
      @Attribute private String display;

      @TagList private List<StepProperty> stepProperties;

      @Getter
      public static class StepProperty {
        @Attribute private String name;
        @Attribute private String value;
      }
    }
  }

  @Getter
  public static class Offset {
    @Attribute private int round;
  }
}
