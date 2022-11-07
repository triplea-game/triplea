package org.triplea.map.data.elements;

import jakarta.xml.bind.annotation.XmlAttribute;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.triplea.generic.xml.reader.annotations.Attribute;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Info {
  /**
   * @deprecated Do not use, game name should be read from map.yml file instead.
   */
  @Deprecated @XmlAttribute @Attribute private String name;
}
