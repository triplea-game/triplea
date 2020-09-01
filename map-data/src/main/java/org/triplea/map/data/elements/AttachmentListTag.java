package org.triplea.map.data.elements;

import java.util.List;
import javax.xml.stream.XMLStreamReader;
import lombok.Builder;
import lombok.Getter;


@Getter
public class AttachmentListTag {
  public static final String TAG_NAME = "attachmentList";

  private final List<Attachment> attachments;

  public AttachmentListTag(final XMLStreamReader streamReader) {
    streamReader.get
  }


  @Builder
  @Getter
  public static class Attachment {
    private final String foreach;
    private final String name;
    private final String attachTo;
    private final String javaClass;
    @Builder.Default
    private final String type = "unitType";

    private final List<Option> options;

    @Builder
    @Getter
    public static class Option {
      private final String name;
      private final String value;
      @Builder.Default
      private final String count = "";
    }
  }
}
