package org.triplea.common.config.product;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;
import org.triplea.common.config.MemoryPropertyReader;
import org.triplea.common.config.product.ProductConfiguration.PropertyKeys;

import games.strategy.util.Version;

final class ProductConfigurationTest {
  private final MemoryPropertyReader memoryPropertyReader = new MemoryPropertyReader();
  private final ProductConfiguration productConfiguration = new ProductConfiguration(memoryPropertyReader);

  @Test
  void getVersion() {
    memoryPropertyReader.setProperty(PropertyKeys.VERSION, "1.0.1.3.buildId");

    assertThat(productConfiguration.getVersion(), is(new Version(1, 0, 1, 3)));
  }
}
