package org.triplea.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import org.jetbrains.annotations.NonNls;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.triplea.java.function.ThrowingSupplier;

final class AbstractInputStreamPropertyReaderTest {
  @Nested
  final class ReadPropertyInternalTest {
    @NonNls private static final String PROPERTY_SOURCE_NAME = "propertySourceName";

    private AbstractInputStreamPropertyReader newAbstractInputStreamPropertyReader(
        final ThrowingSupplier<InputStream, FileNotFoundException> inputStreamSupplier) {
      return new AbstractInputStreamPropertyReader(PROPERTY_SOURCE_NAME) {
        @Override
        protected InputStream newInputStream() throws FileNotFoundException {
          return inputStreamSupplier.get();
        }
      };
    }

    @Test
    void shouldThrowExceptionWhenPropertySourceNotFound() {
      final FileNotFoundException expectedCause = new FileNotFoundException();
      final AbstractInputStreamPropertyReader propertyReader =
          newAbstractInputStreamPropertyReader(
              () -> {
                throw expectedCause;
              });

      final Exception e =
          assertThrows(IllegalStateException.class, () -> propertyReader.readProperty("key"));
      assertThat(e.getMessage()).isEqualTo("Property source not found: " + PROPERTY_SOURCE_NAME);
      assertThat(e.getCause()).isSameAs(expectedCause);
    }

    @Test
    void shouldThrowExceptionWhenPropertySourceReadFails() throws Exception {
      final InputStream is = mock(InputStream.class);
      final IOException expectedCause = new IOException();
      when(is.read(any())).thenThrow(expectedCause);
      final AbstractInputStreamPropertyReader propertyReader =
          newAbstractInputStreamPropertyReader(() -> is);

      final Exception e =
          assertThrows(IllegalStateException.class, () -> propertyReader.readProperty("key"));
      assertThat(e.getMessage())
          .isEqualTo("Failed to read property source: " + PROPERTY_SOURCE_NAME);
      assertThat(e.getCause()).isSameAs(expectedCause);
    }
  }
}
