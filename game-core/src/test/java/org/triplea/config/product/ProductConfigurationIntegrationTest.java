package org.triplea.config.product;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.matchesPattern;

import org.junit.jupiter.api.Test;
import org.triplea.test.common.Integration;

@Integration
final class ProductConfigurationIntegrationTest {
  private final ProductConfiguration productConfiguration = new ProductConfiguration();

  @Test
  void shouldReadPropertiesFromResource() {
    assertThat(productConfiguration.getVersion().toString(), matchesPattern("\\d+\\.\\d+\\.\\d+"));
  }
}
