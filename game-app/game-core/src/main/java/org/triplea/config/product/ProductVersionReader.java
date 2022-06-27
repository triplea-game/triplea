package org.triplea.config.product;

import org.triplea.config.ResourcePropertyReader;
import org.triplea.java.Postconditions;
import org.triplea.util.Version;

/**
 * Provides access to the product configuration. The product configuration applies to all components
 * of the TripleA application suite (e.g. game client, lobby server, etc.).
 */
public final class ProductVersionReader {
  private static Version currentVersion;

  private Version getVersion() {
    var propertyReader = new ResourcePropertyReader("META-INF/triplea/product.properties");
    String versionRead = propertyReader.readProperty("version");
    Postconditions.assertState(
        !versionRead.isBlank(),
        "Failed to read version value from file: META-INF/triplea/product.properties ");
    return new Version((versionRead));
  }

  public static Version getCurrentVersion() {
    if (currentVersion == null) {
      currentVersion = new ProductVersionReader().getVersion();
    }
    return currentVersion;
  }
}
