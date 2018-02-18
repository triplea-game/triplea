package games.strategy.engine.data.util;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Arrays;
import java.util.Objects;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.ResourceCollection;
import games.strategy.engine.data.ResourceList;
import games.strategy.triplea.Constants;

/**
 * A collection of methods that operate on or return resource collections.
 */
public final class ResourceCollectionUtils {
  private ResourceCollectionUtils() {
    // do nothing
  }

  /**
   * Returns a copy of the specified resource collection filtered to exclude the blacklisted resources.
   *
   * @param unfiltered The resource collection to filter; must not be {@code null}.
   * @param resources The blacklisted resources to exclude; must not be {@code null}.
   *
   * @return The filtered resource collection; never {@code null}.
   *
   * @throws IllegalArgumentException If {@code resources} contains a {@code null} element.
   */
  public static ResourceCollection exclude(final ResourceCollection unfiltered, final Resource... resources) {
    checkNotNull(unfiltered);
    checkNotNull(resources);
    checkArgument(Arrays.stream(resources).noneMatch(Objects::isNull), "resources must not contain null");

    final ResourceCollection filtered = new ResourceCollection(unfiltered);
    Arrays.stream(resources).forEach(filtered::removeAllOfResource);
    return filtered;
  }

  /**
   * Returns a copy of the specified resource collection filtered to exclude the blacklisted resources with the given
   * names.
   *
   * @param unfiltered The resource collection to filter; must not be {@code null}.
   * @param names The names of the blacklisted resources to exclude; must not be {@code null}.
   *
   * @return The filtered resource collection; never {@code null}.
   *
   * @throws IllegalArgumentException If {@code names} contains a {@code null} element.
   */
  public static ResourceCollection exclude(final ResourceCollection unfiltered, final String... names) {
    checkNotNull(unfiltered);
    checkNotNull(names);
    checkArgument(Arrays.stream(names).noneMatch(Objects::isNull), "names must not contain null");

    return exclude(unfiltered, mapNamesToResources(unfiltered.getData(), names));
  }

  private static Resource[] mapNamesToResources(final GameData data, final String[] names) {
    data.acquireReadLock();
    try {
      final ResourceList gameResources = data.getResourceList();
      return Arrays.stream(names)
          .map(gameResources::getResource)
          .filter(Objects::nonNull)
          .toArray(Resource[]::new);
    } finally {
      data.releaseReadLock();
    }
  }

  /**
   * Returns a copy of the specified resource collection filtered to include only resources that can be used to make
   * production purchases.
   *
   * @param unfiltered The resource collection to filter; must not be {@code null}.
   *
   * @return The filtered resource collection; never {@code null}.
   */
  public static ResourceCollection getProductionResources(final ResourceCollection unfiltered) {
    checkNotNull(unfiltered);

    return exclude(unfiltered, Constants.TECH_TOKENS, Constants.VPS);
  }
}
