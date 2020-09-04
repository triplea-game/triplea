package org.triplea.map.data.elements;

import java.util.List;
import lombok.Getter;
import org.triplea.generic.xml.reader.Attribute;
import org.triplea.generic.xml.reader.TagList;

@Getter
public class ResourceList {

  @TagList(Resource.class)
  private List<Resource> resources;

  @Getter
  public static class Resource {
    @Attribute private String name;
    @Attribute private String isDisplayedFor;
  }
}
