package org.triplea.config;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.FileNotFoundException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

final class ResourcePropertyReaderTest {
  @Nested
  final class NewInputStreamTest {
    @Test
    void shouldReturnInputStreamForPropertySource() {
      final String resourceName =
          ResourcePropertyReaderTest.class.getName().replace('.', '/') + ".properties";
      final ResourcePropertyReader propertyReader = new ResourcePropertyReader(resourceName);

      assertThat(propertyReader.readProperty("key"), is("value"));
    }

    @Test
    void shouldThrowExceptionWhenResourceNotFound() {
      final String resourceName = "path/to/unknown_resource";
      final ResourcePropertyReader propertyReader = new ResourcePropertyReader(resourceName);

      final Exception e =
          assertThrows(IllegalStateException.class, () -> propertyReader.readProperty("key"));
      assertThat(e.getCause(), is(instanceOf(FileNotFoundException.class)));
      assertThat(e.getCause().getMessage(), is("Resource not found: " + resourceName));
    }
  }
}
