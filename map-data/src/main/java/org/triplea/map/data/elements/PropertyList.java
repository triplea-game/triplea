package org.triplea.map.data.elements;

import java.util.List;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlValue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.triplea.generic.xml.reader.annotations.Attribute;
import org.triplea.generic.xml.reader.annotations.BodyText;
import org.triplea.generic.xml.reader.annotations.LegacyXml;
import org.triplea.generic.xml.reader.annotations.Tag;
import org.triplea.generic.xml.reader.annotations.TagList;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PropertyList {

  @XmlElement(name = "property")
  @TagList
  private List<Property> properties;

  @Getter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Property {
    @XmlAttribute @Attribute private String name;
    @XmlAttribute @Attribute private Boolean editable;
    @XmlAttribute @Attribute private String player;
    @XmlAttribute @Attribute private String value;
    @XmlAttribute @Attribute private Integer min;
    @XmlAttribute @Attribute private Integer max;

    @XmlElement(name = "value")
    @Tag
    private Property.Value valueProperty;

    @XmlElement(name = "number")
    @Tag(names = "number")
    private XmlNumberTag numberProperty;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Value {
      @XmlValue @BodyText private String data;
    }

    @LegacyXml
    @Getter
    public static class XmlNumberTag {
      @XmlAttribute @Attribute private Integer min;
      @XmlAttribute @Attribute private Integer max;
    }
  }
}
