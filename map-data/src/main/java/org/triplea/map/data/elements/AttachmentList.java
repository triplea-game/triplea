package org.triplea.map.data.elements;

import java.util.List;
import lombok.Getter;
import org.triplea.generic.xml.reader.Attribute;
import org.triplea.generic.xml.reader.TagList;

@Getter
public class AttachmentList {
  @TagList(Attachment.class)
  private List<Attachment> attachments;

  @Getter
  public static class Attachment {
    @Attribute private String foreach;
    @Attribute private String name;
    @Attribute private String attachTo;
    @Attribute private String javaClass;

    @Attribute(defaultValue = "unitType")
    private String type;

    @TagList(Option.class)
    private List<Option> options;

    @Getter
    public static class Option {
      @Attribute private String name;
      @Attribute private String value;

      @Attribute(defaultValue = "")
      private String count;
    }
  }
}
