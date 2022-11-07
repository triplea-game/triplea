package org.triplea.map.data.elements;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import java.util.List;
import java.util.Optional;
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
public class VariableList {

  @XmlElement(name = "variable")
  @TagList
  private List<Variable> variables;

  @Getter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Variable {
    @XmlAttribute @Attribute private String name;

    @XmlElement(name = "element")
    @TagList
    private List<Element> elements;

    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Element {
      @XmlAttribute @Attribute private String name;

      public String getName() {
        return Optional.ofNullable(name).orElse("");
      }
    }
  }
}
