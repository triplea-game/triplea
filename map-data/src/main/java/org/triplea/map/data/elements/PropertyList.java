package org.triplea.map.data.elements;

import java.util.List;
import lombok.Getter;
import org.triplea.generic.xml.reader.Attribute;
import org.triplea.generic.xml.reader.BodyText;
import org.triplea.generic.xml.reader.Tag;
import org.triplea.generic.xml.reader.TagList;

@Getter
public class PropertyList {

  @TagList(Property.class)
  private List<Property> properties;

  @Getter
  public static class Property {
    @Attribute(defaultValue = "")
    private String value;

    @Attribute private String name;

    @Attribute(defaultValue = "false")
    private String editable;

    @Attribute(defaultValue = "")
    private String player;

    @Tag private Property.Value valueProperty;
    @Tag private Property.Boolean booleanProperty;
    @Tag private Property.String stringProperty;
    @Tag private Property.Number numberProperty;

    public static class Value {
      @BodyText private String data;
    }

    public static class Boolean {}

    public static class String {}

    @Getter
    public static class Number {
      @Attribute private String min;
      @Attribute private String max;
    }
  }
}
