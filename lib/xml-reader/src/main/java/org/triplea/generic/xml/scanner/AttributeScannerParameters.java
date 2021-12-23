package org.triplea.generic.xml.scanner;

import javax.annotation.Nonnull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter(AccessLevel.PACKAGE)
public class AttributeScannerParameters {
  @Nonnull private final String tag;
  @Nonnull private final String attributeName;
}
