package org.triplea.map.data.elements;

import java.util.Optional;
import lombok.Getter;
import org.triplea.generic.xml.reader.annotations.Tag;

/** Contains a partial subset of a full 'Game' object. */
@Getter
public class ShallowParsedGame {
  @Tag private Info info;
  @Tag private PlayerList playerList;
  @Tag private PropertyList propertyList;

  public Optional<PropertyList.Property> getProperty(final String propertyName) {
    return propertyList.getProperties().stream()
        .filter(property -> property.getName().equalsIgnoreCase(propertyName))
        .findAny();
  }
}
