package org.triplea.map.data.elements;

import java.util.List;
import lombok.Getter;
import org.triplea.generic.xml.reader.annotations.Attribute;
import org.triplea.generic.xml.reader.annotations.TagList;

@Getter
public class ResourceList {

  @TagList private List<Resource> resources;

  @Getter
  public static class Resource {
    @Attribute private String name;
    @Attribute private String isDisplayedFor;
  }
}
