package org.triplea.map.data.elements;

import java.util.List;
import lombok.Getter;
import org.triplea.generic.xml.reader.annotations.Attribute;
import org.triplea.generic.xml.reader.annotations.TagList;

@Getter
public class RelationshipTypes {

  @TagList private List<RelationshipType> relationshipTypes;

  @Getter
  public static class RelationshipType {
    @Attribute private String name;
  }
}
