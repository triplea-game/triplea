package org.triplea.generic.xml.reader;

import java.io.InputStream;
import lombok.experimental.UtilityClass;

@UtilityClass
class XmlMapperTestUtils {
  InputStream openFile(final String fileName) {
    return XmlMapperTestUtils.class.getClassLoader().getResourceAsStream(fileName);
  }
}
