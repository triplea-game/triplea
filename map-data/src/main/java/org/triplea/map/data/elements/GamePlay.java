package org.triplea.map.data.elements;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import org.triplea.map.reader.generic.xml.Attribute;
import org.triplea.map.reader.generic.xml.Tag;
import org.triplea.map.reader.generic.xml.TagList;

@Getter
public class GamePlay {

  @TagList(Delegate.class)
  private List<Delegate> delegates = new ArrayList<>();

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
    @TagList(Step.class)
    private List<Step> steps = new ArrayList<>();

    @Getter
    public static class Step {
      @Attribute private String name;
      @Attribute private String delegate;
      @Attribute private String player;
      @Attribute private String maxRunCount;
      @Attribute private String display;

      @TagList(StepProperty.class)
      private List<StepProperty> stepProperties = new ArrayList<>();

      @Getter
      public static class StepProperty {
        @Attribute private String name;
        @Attribute private String value;
      }
    }
  }

  @Getter
  public static class Offset {
    @Attribute private String round;
  }
}
