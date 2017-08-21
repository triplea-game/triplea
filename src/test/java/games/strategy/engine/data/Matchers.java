package games.strategy.engine.data;

import javax.annotation.Nullable;

import org.hamcrest.Matcher;

import games.strategy.test.EqualityComparatorRegistry;

/**
 * A collection of matchers for types in the {@link games.strategy.engine.data} package.
 */
public final class Matchers {
  private Matchers() {}

  /**
   * Creates a matcher that matches when the examined {@link GameData} is logically equal to the specified
   * {@link GameData}.
   *
   * @param expected The expected {@link GameData} value.
   *
   * @return A new matcher.
   */
  public static Matcher<GameData> equalToGameData(final @Nullable GameData expected) {
    final EqualityComparatorRegistry equalityComparatorRegistry =
        EqualityComparatorRegistry.newInstance(TestEqualityComparatorCollectionBuilder.forGameData().build());
    return games.strategy.test.Matchers.equalTo(expected).withEqualityComparatorRegistry(equalityComparatorRegistry);
  }
}
