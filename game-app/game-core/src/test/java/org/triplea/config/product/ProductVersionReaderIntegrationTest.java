package org.triplea.config.product;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

final class ProductVersionReaderIntegrationTest {
  @Test
  void shouldReadPropertiesFromResource() {
    assertThat(ProductVersionReader.getCurrentVersion().toString()).matches("\\d+\\.\\d+\\+.*");
  }
}
