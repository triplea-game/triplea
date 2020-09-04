package org.triplea.map.data.elements;

import java.util.List;
import lombok.Getter;
import org.triplea.generic.xml.reader.Attribute;
import org.triplea.generic.xml.reader.TagList;

@Getter
public class VariableList {

  @TagList(Variable.class)
  private List<Variable> variables;

  @Getter
  public static class Variable {
    @Attribute private String name;

    @TagList(Element.class)
    private List<Element> elements;

    @Getter
    public static class Element {
      @Attribute private String name;
    }
  }
}
