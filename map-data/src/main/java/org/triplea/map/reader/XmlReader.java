package org.triplea.map.reader;

import com.google.common.base.Preconditions;
import java.util.Optional;
import javax.annotation.Nonnull;
import javax.xml.stream.XMLStreamReader;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public class XmlReader {
  @Getter
  private final XMLStreamReader xmlStreamReader;

  public String getAttributeValue(@Nonnull final String attributeName) {
    Preconditions.checkNotNull(attributeName);
    return xmlStreamReader.getAttributeValue(null, attributeName);
  }

  public String getAttributeValue(
      @Nonnull final String attributeName, @Nonnull final String defaultValue) {
    Preconditions.checkNotNull(attributeName);
    Preconditions.checkNotNull(defaultValue);
    return Optional.ofNullable(xmlStreamReader.getAttributeValue(null, attributeName))
        .orElse(defaultValue);
  }
}
