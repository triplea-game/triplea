package org.triplea.config.product;

import com.google.common.annotations.VisibleForTesting;
import org.triplea.config.PropertyReader;
import org.triplea.config.ResourcePropertyReader;
import org.triplea.util.Version;

/**
 * Provides access to the product configuration. The product configuration applies to all components
 * of the TripleA application suite (e.g. game client, lobby server, etc.).
 */
public final class ProductVersionReader {
  private final PropertyReader propertyReader;

  public ProductVersionReader() {
    this(new ResourcePropertyReader("META-INF/triplea/product.properties"));
  }

  @VisibleForTesting
  ProductVersionReader(final PropertyReader propertyReader) {
    this.propertyReader = propertyReader;
  }

  public Version getVersion() {
    return new Version(propertyReader.readProperty(PropertyKeys.VERSION));
  }

  @VisibleForTesting
  interface PropertyKeys {
    String VERSION = "version";
  }
}
