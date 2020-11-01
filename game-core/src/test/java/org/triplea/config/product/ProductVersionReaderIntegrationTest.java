package org.triplea.config.product;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.matchesPattern;

import org.junit.jupiter.api.Test;
import org.triplea.test.common.Integration;

@Integration
final class ProductVersionReaderIntegrationTest {
  private final ProductVersionReader productVersionReader = new ProductVersionReader();

  @Test
  void shouldReadPropertiesFromResource() {
    assertThat(productVersionReader.getVersion().toString(), matchesPattern("\\d+\\.\\d+\\.\\d+"));
  }
}
