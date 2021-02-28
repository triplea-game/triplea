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
public class ResourceList {

  @XmlElement(name = "resource")
  @TagList
  private List<Resource> resources;

  @Getter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Resource {
    @XmlAttribute @Attribute private String name;
    @XmlAttribute @Attribute private String isDisplayedFor;
  }
}
