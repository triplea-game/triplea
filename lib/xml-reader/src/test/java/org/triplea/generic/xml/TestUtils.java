package org.triplea.generic.xml;

import java.io.InputStream;
import lombok.experimental.UtilityClass;

@UtilityClass
public class TestUtils {
  public InputStream openFile(final String fileName) {
    return TestUtils.class.getClassLoader().getResourceAsStream(fileName);
  }
}
