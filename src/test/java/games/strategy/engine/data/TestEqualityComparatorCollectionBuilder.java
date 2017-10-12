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
        // --- main ---
        .add(CoreEqualityComparators.COLLECTION)
        .add(CoreEqualityComparators.INTEGER_MAP)
        .add(CoreEqualityComparators.MAP)
        .add(EngineDataEqualityComparators.AA_RADAR_ADVANCE)
        .add(EngineDataEqualityComparators.DESTROYER_BOMBARD_TECH_ADVANCE)
        .add(EngineDataEqualityComparators.GAME_DATA)
        .add(EngineDataEqualityComparators.HEAVY_BOMBER_ADVANCE)
        .add(EngineDataEqualityComparators.IMPROVED_ARTILLERY_SUPPORT_ADVANCE)
        .add(EngineDataEqualityComparators.IMPROVED_SHIPYARDS_ADVANCE)
        .add(EngineDataEqualityComparators.INCREASED_FACTORY_PRODUCTION_ADVANCE)
        .add(EngineDataEqualityComparators.INDUSTRIAL_TECHNOLOGY_ADVANCE)
        .add(EngineDataEqualityComparators.JET_POWER_ADVANCE)
        .add(EngineDataEqualityComparators.LONG_RANGE_AIRCRAFT_ADVANCE)
        .add(EngineDataEqualityComparators.MECHANIZED_INFANTRY_ADVANCE)
        .add(EngineDataEqualityComparators.PARATROOPERS_ADVANCE)
        .add(EngineDataEqualityComparators.PLAYER_ID)
        .add(EngineDataEqualityComparators.PLAYER_LIST)
        .add(EngineDataEqualityComparators.PRODUCTION_FRONTIER)
        .add(EngineDataEqualityComparators.PRODUCTION_RULE)
        .add(EngineDataEqualityComparators.REPAIR_FRONTIER)
        .add(EngineDataEqualityComparators.REPAIR_RULE)
        .add(EngineDataEqualityComparators.RESOURCE)
        .add(EngineDataEqualityComparators.RESOURCE_COLLECTION)
        .add(EngineDataEqualityComparators.ROCKETS_ADVANCE)
        .add(EngineDataEqualityComparators.SUPER_SUBS_ADVANCE)
        .add(EngineDataEqualityComparators.TECHNOLOGY_FRONTIER)
        .add(EngineDataEqualityComparators.TECHNOLOGY_FRONTIER_LIST)
        .add(EngineDataEqualityComparators.UNIT)
        .add(EngineDataEqualityComparators.UNIT_TYPE)
        .add(EngineDataEqualityComparators.WAR_BONDS_ADVANCE)
        // --- test ---
        .add(EngineDataEqualityComparators.FAKE_TECH_ADVANCE);
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
