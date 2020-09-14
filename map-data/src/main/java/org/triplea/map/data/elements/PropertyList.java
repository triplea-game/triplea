package org.triplea.map.data.elements;

import java.util.List;
import lombok.Getter;
import org.triplea.generic.xml.reader.annotations.Attribute;
import org.triplea.generic.xml.reader.annotations.BodyText;
import org.triplea.generic.xml.reader.annotations.LegacyXml;
import org.triplea.generic.xml.reader.annotations.Tag;
import org.triplea.generic.xml.reader.annotations.TagList;

@Getter
public class PropertyList {

  @TagList private List<Property> properties;

  @Getter
  public static class Property {
    @Attribute private String name;
    @Attribute private boolean editable;
    @Attribute private String player;
    @Attribute private String value;
    @Attribute private Integer min;
    @Attribute private Integer max;

    @Tag private Property.Value valueProperty;

    @Tag(names = "Number")
    private XmlNumberTag numberProperty;

    @Getter
    public static class Value {
      @BodyText private String data;
    }

    @LegacyXml
    @Getter
    public static class XmlNumberTag {
      @Attribute private int min;
      @Attribute private int max;
    }
  }
}
