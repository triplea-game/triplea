package org.triplea.config;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;
import org.triplea.io.IoUtils;
import org.triplea.test.common.ExtendedUnitTest;

@ExtendedUnitTest
final class AbstractInputStreamPropertyReaderAsPropertyReaderTest
    extends AbstractPropertyReaderTestCase {
  @Override
  protected PropertyReader newPropertyReader(final Map<String, String> properties)
      throws Exception {
    final Properties props = new Properties();
    props.putAll(properties);
    final byte[] bytes = IoUtils.writeToMemory(os -> props.store(os, null));

    return new AbstractInputStreamPropertyReader("propertySourceName") {
      @Override
      protected InputStream newInputStream() {
        return new ByteArrayInputStream(bytes);
      }
    };
  }
}
