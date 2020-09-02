package org.triplea.map.data.elements;

import java.util.ArrayList;
import java.util.List;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import lombok.Getter;
import org.triplea.map.reader.XmlParser;

@Getter
public class GamePlay {
  static final String TAG_NAME = "gamePlay";

  private List<Delegate> delegates = new ArrayList<>();
  private Sequence sequence;
  private Offset offset;

  GamePlay(final XMLStreamReader streamReader) throws XMLStreamException {
    XmlParser.tag(TAG_NAME)
        .addChildTagHandler(
            Delegate.TAG_NAME, () -> delegates.add(new Delegate(streamReader)))
        .addChildTagHandler(Sequence.TAG_NAME, () -> sequence = new Sequence(streamReader))
        .addChildTagHandler(Offset.TAG_NAME, () -> offset = new Offset(streamReader))
        .parse(streamReader);
  }

  @Getter
  public static class Delegate {
    public static final String TAG_NAME = "delegate";

    private String name;
    private String javaClass;
    private String display;

    Delegate(final XMLStreamReader streamReader) throws XMLStreamException {
      XmlParser.tag(TAG_NAME)
          .addAttributeHandler("name", value -> name = value)
          .addAttributeHandler("javaClass", value -> javaClass = value)
          .addAttributeHandler("display", value -> display = value)
          .parse(streamReader);
    }
  }

  @Getter
  public static class Sequence {
    public static final String TAG_NAME = "sequence";

    private List<Step> steps = new ArrayList<>();

    Sequence(final XMLStreamReader streamReader) throws XMLStreamException {
      XmlParser.tag(TAG_NAME)
          .addChildTagHandler("step", () -> steps.add(new Step(streamReader)))
          .parse(streamReader);
    }

    @Getter
    public static class Step {
      public static final String TAG_NAME = "step";

      private String name;
      private String delegate;
      private String player;
      private String maxRunCount;
      private String display;

      private List<StepProperty> stepProperties = new ArrayList<>();

      Step(final XMLStreamReader streamReader) throws XMLStreamException {
        XmlParser.tag(TAG_NAME)
            .addAttributeHandler("name", value -> name = value)
            .addAttributeHandler("delegate", value -> delegate = value)
            .addAttributeHandler("player", value -> player = value)
            .addAttributeHandler("maxRunCount", value -> maxRunCount = value)
            .addAttributeHandler("display", value -> display = value)
            .addChildTagHandler(
                "stepProperty", () -> stepProperties.add(new StepProperty(streamReader)))
            .parse(streamReader);
      }

      @Getter
      public static class StepProperty {
        public static final String TAG_NAME = "stepProperty";
        private String name;
        private String value;

        StepProperty(final XMLStreamReader streamReader) throws XMLStreamException {
          XmlParser.tag(TAG_NAME)
              .addAttributeHandler("name", value -> name = value)
              .addAttributeHandler("value", attributeValue -> value = attributeValue)
              .parse(streamReader);
        }
      }
    }
  }

  @Getter
  public static class Offset {
    public static final String TAG_NAME = "offset";
    private String round;

    Offset(final XMLStreamReader streamReader) throws XMLStreamException {
      XmlParser.tag(TAG_NAME)
          .addAttributeHandler("round", value -> round = value)
          .parse(streamReader);
    }
  }
}
