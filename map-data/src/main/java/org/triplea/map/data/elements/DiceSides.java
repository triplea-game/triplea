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
public class DiceSides {
  @XmlAttribute @Attribute private Integer value;
}
