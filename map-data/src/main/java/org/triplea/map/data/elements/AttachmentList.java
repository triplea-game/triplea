package org.triplea.map.data.elements;

import java.util.ArrayList;
import java.util.List;
import javax.xml.stream.XMLStreamException;
import lombok.Getter;
import org.triplea.map.reader.XmlParser;
import org.triplea.map.reader.XmlReader;

@Getter
public class AttachmentList {
  public static final String TAG_NAME = "attachmentList";

  private final List<Attachment> attachments;

  AttachmentList(final XmlReader streamReader) throws XMLStreamException {
    attachments = new ArrayList<>();

    XmlParser.tag(TAG_NAME)
        .childTagHandler(Attachment.TAG_NAME, () -> attachments.add(new Attachment(streamReader)))
        .parse(streamReader);
  }

  @Getter
  public static class Attachment {
    static final String TAG_NAME = "attachment";

    private final String foreach;
    private final String name;
    private final String attachTo;
    private final String javaClass;
    private final String type;

    private final List<Option> options = new ArrayList<>();

    public Attachment(final XmlReader xmlReader) throws XMLStreamException {
      foreach = xmlReader.getAttributeValue("foreach");
      name = xmlReader.getAttributeValue("name");
      attachTo = xmlReader.getAttributeValue("attachTo");
      javaClass = xmlReader.getAttributeValue("javaClass");
      type = xmlReader.getAttributeValue("type", "unitType");

      XmlParser.tag(TAG_NAME)
          .childTagHandler(Option.TAG_NAME, () -> options.add(new Option(xmlReader)))
          .parse(xmlReader);
    }

    @Getter
    public static class Option {
      private static final String TAG_NAME = "option";

      private final String name;
      private final String value;
      private final String count;

      public Option(final XmlReader streamReader) {
        name = streamReader.getAttributeValue("name");
        value = streamReader.getAttributeValue("value");
        count = streamReader.getAttributeValue("count", "");
      }
    }
  }
}
