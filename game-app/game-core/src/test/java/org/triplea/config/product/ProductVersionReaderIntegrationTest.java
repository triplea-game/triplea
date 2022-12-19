package org.triplea.config.product;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.matchesPattern;

import org.junit.jupiter.api.Test;

final class ProductVersionReaderIntegrationTest {
  @Test
  void shouldReadPropertiesFromResource() {
    assertThat(
        ProductVersionReader.getCurrentVersion().toString(), matchesPattern("\\d+\\.\\d+\\+.*"));
  }
}
