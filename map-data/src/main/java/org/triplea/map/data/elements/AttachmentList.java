package org.triplea.map.data.elements;

import java.util.ArrayList;
import java.util.List;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import lombok.Getter;
import org.triplea.map.reader.XmlParser;

@Getter
public class AttachmentList {
  public static final String TAG_NAME = "attachmentList";

  private List<Attachment> attachments;

  AttachmentList(final XMLStreamReader streamReader) throws XMLStreamException {
    attachments = new ArrayList<>();

    new XmlParser(TAG_NAME)
        .addChildTagHandler(
            Attachment.TAG_NAME, () -> attachments.add(new Attachment(streamReader)))
        .parse(streamReader);
  }

  @Getter
  public static class Attachment {
    static final String TAG_NAME = "attachment";

    private String foreach;
    private String name;
    private String attachTo;
    private String javaClass;
    private String type = "unitType";

    private List<Option> options = new ArrayList<>();

    public Attachment(final XMLStreamReader streamReader) throws XMLStreamException {
      new XmlParser(TAG_NAME)
          .addAttributeHandler("foreach", value -> foreach = value)
          .addAttributeHandler("name", value -> name = value)
          .addAttributeHandler("attachTo", value -> attachTo = value)
          .addAttributeHandler("javaClass", value -> javaClass = value)
          .addAttributeHandler("type", value -> type = value)
          .addChildTagHandler(Option.TAG_NAME, () -> options.add(new Option(streamReader)))
          .parse(streamReader);
    }

    @Getter
    public static class Option {
      private static final String TAG_NAME = "option";

      private String name;
      private String value;
      private String count = "";

      public Option(final XMLStreamReader streamReader) throws XMLStreamException {
        new XmlParser(TAG_NAME)
            .addAttributeHandler("name", attributeValue -> name = attributeValue)
            .addAttributeHandler("value", attributeValue -> value = attributeValue)
            .addAttributeHandler("count", attributeValue -> count = attributeValue)
            .parse(streamReader);
      }
    }
  }
}
