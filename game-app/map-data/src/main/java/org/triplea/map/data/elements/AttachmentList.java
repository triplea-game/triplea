package org.triplea.map.data.elements;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.triplea.generic.xml.reader.annotations.Attribute;
import org.triplea.generic.xml.reader.annotations.TagList;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttachmentList {
  @TagList(names = {"Attachment", "Attatchment"})
  @XmlElement(name = "attachment")
  private List<Attachment> attachments;

  @Getter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Attachment {
    @XmlAttribute @Attribute private String foreach;
    @XmlAttribute @Attribute private String name;

    @Attribute(names = {"attachTo", "attatchTo"})
    @XmlAttribute
    private String attachTo;

    @XmlAttribute @Attribute private String javaClass;

    @XmlAttribute @Attribute private String type;

    @XmlElement(name = "option")
    @TagList
    private List<Option> options;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Option {
      @XmlAttribute @Attribute private String name;
      @XmlAttribute @Attribute private String value;

      @XmlAttribute @Attribute private String count;
    }
  }
}
