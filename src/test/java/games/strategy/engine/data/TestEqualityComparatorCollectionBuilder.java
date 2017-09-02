package games.strategy.engine.data;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Collection;

import games.strategy.test.EqualityComparator;
import games.strategy.util.CoreEqualityComparators;

/**
 * Provides support for incrementally building equality comparator collections typically required by test fixtures.
 */
public final class TestEqualityComparatorCollectionBuilder {
  private final Collection<EqualityComparator> equalityComparators = new ArrayList<>();

  private TestEqualityComparatorCollectionBuilder() {}

  /**
   * Creates a new equality comparator collection builder that is pre-populated with all equality comparators required
   * to compare instances of {@code GameData} for equality.
   *
   * @return A new equality comparator collection builder.
   */
  public static TestEqualityComparatorCollectionBuilder forGameData() {
    return new TestEqualityComparatorCollectionBuilder()
        .add(CoreEqualityComparators.COLLECTION)
        .add(CoreEqualityComparators.MAP)
        .add(EngineDataEqualityComparators.GAME_DATA);
  }

  /**
   * Adds the specified equality comparator to the collection under construction.
   *
   * @param equalityComparator The equality comparator to add.
   *
   * @return A reference to this builder.
   */
  public TestEqualityComparatorCollectionBuilder add(final EqualityComparator equalityComparator) {
    checkNotNull(equalityComparator);

    equalityComparators.add(equalityComparator);
    return this;
  }

  /**
   * Adds the specified collection of equality comparators to the collection under construction.
   *
   * @param equalityComparators The collection of equality comparators to add.
   *
   * @return A reference to this builder.
   */
  public TestEqualityComparatorCollectionBuilder addAll(final Collection<EqualityComparator> equalityComparators) {
    checkNotNull(equalityComparators);

    this.equalityComparators.addAll(equalityComparators);
    return this;
  }

  public Collection<EqualityComparator> build() {
    return new ArrayList<>(equalityComparators);
  }
}
