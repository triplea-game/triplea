package org.triplea.map.data.elements;

import java.util.List;
import lombok.Getter;
import org.triplea.generic.xml.reader.annotations.Attribute;
import org.triplea.generic.xml.reader.annotations.TagList;

@Getter
public class VariableList {

  @TagList private List<Variable> variables;

  @Getter
  public static class Variable {
    @Attribute private String name;

    @TagList private List<Element> elements;

    @Getter
    public static class Element {
      @Attribute private String name;
    }
  }
}
