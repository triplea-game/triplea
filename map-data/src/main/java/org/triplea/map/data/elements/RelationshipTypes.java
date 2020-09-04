package org.triplea.map.data.elements;

import java.util.List;
import lombok.Getter;
import org.triplea.generic.xml.reader.Attribute;
import org.triplea.generic.xml.reader.TagList;

@Getter
public class RelationshipTypes {

  @TagList(RelationshipType.class)
  private List<RelationshipType> relationshipTypes;

  @Getter
  public static class RelationshipType {
    @Attribute private String name;
  }
}
