package org.triplea.map.data.elements;

import java.util.ArrayList;
import java.util.List;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import lombok.Builder;
import lombok.Getter;

@Getter
public class AttachmentListTag {
  public static final String TAG_NAME = "attachmentList";

  private List<Attachment> attachments;

  public AttachmentListTag(final XMLStreamReader streamReader) throws XMLStreamException {
    attachments = new ArrayList<>();
    boolean endTagReached = false;
    while (streamReader.hasNext() && !endTagReached) {
      final int event = streamReader.next();
      switch (event) {
        case XMLStreamReader.START_ELEMENT:
          switch (streamReader.getLocalName()) {
            case Attachment.TAG_NAME:
              attachments.add(new Attachment(streamReader));
              break;
          }
          break;
        case XMLStreamReader.END_ELEMENT:
          if (streamReader.getLocalName().equals(TAG_NAME)) {
            endTagReached = true;
          }
          break;
      }
    }
  }

  @Getter
  public static class Attachment {
    static final String TAG_NAME = "attachment";

    private String foreach;
    private String name;
    private String attachTo;
    private String javaClass;
    @Builder.Default private final String type = "unitType";

    private List<Option> options;

    public Attachment(final XMLStreamReader streamReader) {


    }

    @Builder
    @Getter
    public static class Option {
      private final String name;
      private final String value;
      @Builder.Default private final String count = "";
    }
  }
}
