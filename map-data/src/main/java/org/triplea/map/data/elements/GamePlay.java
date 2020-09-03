package org.triplea.map.data.elements;

import java.util.ArrayList;
import java.util.List;
import javax.xml.stream.XMLStreamException;
import lombok.Getter;
import org.triplea.map.reader.XmlParser;
import org.triplea.map.reader.XmlReader;

@Getter
public class GamePlay {
  static final String TAG_NAME = "gamePlay";

  private final List<Delegate> delegates = new ArrayList<>();
  private Sequence sequence;
  private Offset offset;

  GamePlay(final XmlReader streamReader) throws XMLStreamException {
    XmlParser.tag(TAG_NAME)
        .childTagHandler(Delegate.TAG_NAME, () -> delegates.add(new Delegate(streamReader)))
        .childTagHandler(Sequence.TAG_NAME, () -> sequence = new Sequence(streamReader))
        .childTagHandler(Offset.TAG_NAME, () -> offset = new Offset(streamReader))
        .parse(streamReader);
  }

  @Getter
  public static class Delegate {
    public static final String TAG_NAME = "delegate";

    private final String name;
    private final String javaClass;
    private final String display;

    Delegate(final XmlReader streamReader) {
      name = streamReader.getAttributeValue("name");
      javaClass = streamReader.getAttributeValue("javaClass");
      display = streamReader.getAttributeValue("display");
    }
  }

  @Getter
  public static class Sequence {
    public static final String TAG_NAME = "sequence";

    private final List<Step> steps = new ArrayList<>();

    Sequence(final XmlReader streamReader) throws XMLStreamException {
      XmlParser.tag(TAG_NAME)
          .childTagHandler("step", () -> steps.add(new Step(streamReader)))
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

      private final List<StepProperty> stepProperties = new ArrayList<>();

      Step(final XmlReader streamReader) throws XMLStreamException {
        name = streamReader.getAttributeValue("name");
        delegate = streamReader.getAttributeValue("delegate");
        player = streamReader.getAttributeValue("player");
        maxRunCount = streamReader.getAttributeValue("maxRunCount");
        display = streamReader.getAttributeValue("display");

        XmlParser.tag(TAG_NAME)
            .childTagHandler(
                "stepProperty", () -> stepProperties.add(new StepProperty(streamReader)))
            .parse(streamReader);
      }

      @Getter
      public static class StepProperty {
        public static final String TAG_NAME = "stepProperty";
        private String name;
        private String value;

        StepProperty(final XmlReader xmlReader) {
          name = xmlReader.getAttributeValue("name");
          value = xmlReader.getAttributeValue("value");
        }
      }
    }
  }

  @Getter
  public static class Offset {
    public static final String TAG_NAME = "offset";
    private String round;

    Offset(final XmlReader xmlReader) {
      round = xmlReader.getAttributeValue("round");
    }
  }
}
