package org.triplea.generic.xml.scanner;

import javax.annotation.Nonnull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter(AccessLevel.PACKAGE)
public class BodyTextScannerParameters {
  @Nonnull private final String parentTag;
  @Nonnull private final String parentTagAttributeName;
  @Nonnull private final String parentTagAttributeValue;
  @Nonnull private final String childTag;
}
