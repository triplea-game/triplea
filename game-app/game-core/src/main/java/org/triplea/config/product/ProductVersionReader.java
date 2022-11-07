package org.triplea.config.product;

import org.triplea.config.ResourcePropertyReader;
import org.triplea.util.Version;

/**
 * Provides access to the product configuration. The product configuration applies to all components
 * of the TripleA application suite (e.g. game client, lobby server, etc.).
 */
public final class ProductVersionReader {
  private static Version currentVersion;

  public static Version getCurrentVersion() {
    if (currentVersion == null) {
      var resourcePropertyReader =
          new ResourcePropertyReader("META-INF/triplea/product.properties");
      currentVersion = new Version(resourcePropertyReader.readProperty("version"));
    }
    return currentVersion;
  }
}
