package org.triplea.map.data.elements;

import java.util.List;
import lombok.Getter;
import org.triplea.generic.xml.reader.annotations.Attribute;
import org.triplea.generic.xml.reader.annotations.BodyText;
import org.triplea.generic.xml.reader.annotations.Tag;
import org.triplea.generic.xml.reader.annotations.TagList;

@Getter
public class PropertyList {

  @TagList private List<Property> properties;

  @Getter
  public static class Property {
    @Attribute private java.lang.String name;

    @Attribute(defaultValue = "false")
    private java.lang.String editable;

    @Attribute private java.lang.String player;

    @Attribute private java.lang.String value;

    @Tag private Property.Value valueProperty;
    @Tag private Property.Boolean booleanProperty;
    @Tag private Property.String stringProperty;
    @Tag private Property.Number numberProperty;

    @Getter
    public static class Value {
      @BodyText private java.lang.String data;
    }

    public static class Boolean {}

    public static class String {}

    @Getter
    public static class Number {
      @Attribute private int min;
      @Attribute private int max;
    }
  }
}
