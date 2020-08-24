package games.strategy.engine;

import games.strategy.engine.framework.map.download.DownloadCoordinator;
import org.triplea.config.product.ProductConfiguration;
import org.triplea.util.Version;

/**
 * Manages the creation of objects, similar to a dependency injection framework. Use this class to
 * manage singletons and as a factory to create objects that have shared dependencies already
 * managed by this class. Example usage:
 *
 * <pre>
 * <code>
 *   // before
 *   public void clientCode(SharedDependency sharedDependencyWiredThroughAllTheMethods) {
 *     swingStuff(sharedDependencyWiredThroughAllTheMethods);
 *     :
 *   }
 *   private void swingStuff(SharedDependency sharedDependencyWiredThroughAllTheMethods) {
 *     int preferenceValue =
 *         new UserSetting(sharedDependencyWiredThroughAllTheMethods).getNumberPreference();
 *     :
 *   }
 *
 *   // after
 *   public void clientCode() {
 *     doSwingStuff(ClientContext.userSettings());
 *     :
 *   }
 *
 *   private void doSwingStuff(UserSettings settings) {
 *     int preferenceValue = settings.getNumberPreference();
 *     :
 *   }
 * </code>
 * </pre>
 */
public final class ClientContext {
  private static final ClientContext instance = new ClientContext();

  private final ProductConfiguration productConfiguration = new ProductConfiguration();
  private final DownloadCoordinator downloadCoordinator = new DownloadCoordinator();

  private ClientContext() {}

  public static DownloadCoordinator downloadCoordinator() {
    return instance.downloadCoordinator;
  }

  public static Version engineVersion() {
    return instance.productConfiguration.getVersion();
  }
}
