package org.triplea.map.data.elements;

import javax.xml.bind.annotation.XmlAttribute;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.triplea.generic.xml.reader.annotations.Attribute;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Info {
  @XmlAttribute @Attribute private String name;
  @XmlAttribute @Attribute private String version;
}
