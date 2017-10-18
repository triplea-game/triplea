package games.strategy.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableMap;

import games.strategy.test.EqualityComparator;
import games.strategy.test.EqualityComparatorRegistry;
import games.strategy.test.TestUtil;

public final class CoreEqualityComparatorsTests {
  private static EqualityComparatorRegistry newEqualityComparatorRegistryOf(
      final EqualityComparator primaryEqualityComparator,
      final EqualityComparator... secondaryEqualityComparators) {
    final Collection<EqualityComparator> equalityComparators = new ArrayList<>();
    equalityComparators.add(primaryEqualityComparator);
    equalityComparators.addAll(Arrays.asList(secondaryEqualityComparators));
    return EqualityComparatorRegistry.newInstance(equalityComparators);
  }

  @Nested
  public final class CollectionTest {
    private final EqualityComparatorRegistry equalityComparatorRegistry = newEqualityComparatorRegistry();

    private EqualityComparatorRegistry newEqualityComparatorRegistry(
        final EqualityComparator... equalityComparators) {
      return newEqualityComparatorRegistryOf(CoreEqualityComparators.COLLECTION, equalityComparators);
    }

    @Test
    public void shouldReturnFalseWhenCollectionsHaveDifferentSizes() {
      assertFalse(TestUtil.equals(
          equalityComparatorRegistry,
          Arrays.asList(new Integer(1)),
          Arrays.asList(new Integer(1), new Integer(2))));
    }

    @Test
    public void shouldReturnTrueWhenEqual() {
      assertTrue(TestUtil.equals(
          equalityComparatorRegistry,
          Arrays.asList(new Integer(1), new Integer(2)),
          Arrays.asList(new Integer(1), new Integer(2))));
    }

    @Test
    public void shouldReturnFalseWhenNotEqual() {
      assertFalse(TestUtil.equals(
          equalityComparatorRegistry,
          Arrays.asList(new Integer(1), new Integer(2)),
          Arrays.asList(new Integer(1), new Integer(-2))));
    }

    @Test
    public void shouldUseEqualityComparatorForElements() {
      final EqualityComparatorRegistry equalityComparatorRegistry = newEqualityComparatorRegistry(
          EqualityComparator.newInstance(Integer.class, (context, o1, o2) -> false));

      assertFalse(TestUtil.equals(
          equalityComparatorRegistry,
          Arrays.asList(new Integer(1), new Integer(2)),
          Arrays.asList(new Integer(1), new Integer(2))));
    }
  }

  @Nested
  public final class MapTest {
    private final EqualityComparatorRegistry equalityComparatorRegistry = newEqualityComparatorRegistry();

    private EqualityComparatorRegistry newEqualityComparatorRegistry(
        final EqualityComparator... equalityComparators) {
      return newEqualityComparatorRegistryOf(CoreEqualityComparators.MAP, equalityComparators);
    }

    @Test
    public void shouldReturnFalseWhenMapsHaveDifferentSizes() {
      assertFalse(TestUtil.equals(
          equalityComparatorRegistry,
          ImmutableMap.of(new Integer(1), new Boolean(true)),
          ImmutableMap.of(new Integer(1), new Boolean(true), new Integer(2), new Boolean(true))));
    }

    @Test
    public void shouldReturnTrueWhenEqual() {
      assertTrue(TestUtil.equals(
          equalityComparatorRegistry,
          ImmutableMap.of(new Integer(1), new Boolean(true), new Integer(2), new Boolean(true)),
          ImmutableMap.of(new Integer(1), new Boolean(true), new Integer(2), new Boolean(true))));
    }

    @Test
    public void shouldReturnFalseWhenNotEqual() {
      assertFalse(TestUtil.equals(
          equalityComparatorRegistry,
          ImmutableMap.of(new Integer(1), new Boolean(true), new Integer(2), new Boolean(true)),
          ImmutableMap.of(new Integer(1), new Boolean(true), new Integer(2), new Boolean(false))));
    }

    @Test
    public void shouldUseEqualityComparatorForKeys() {
      final EqualityComparatorRegistry equalityComparatorRegistry = newEqualityComparatorRegistry(
          EqualityComparator.newInstance(Integer.class, (context, o1, o2) -> false));

      assertFalse(TestUtil.equals(
          equalityComparatorRegistry,
          ImmutableMap.of(new Integer(1), new Boolean(true), new Integer(2), new Boolean(true)),
          ImmutableMap.of(new Integer(1), new Boolean(true), new Integer(2), new Boolean(true))));
    }

    @Test
    public void shouldUseEqualityComparatorForValues() {
      final EqualityComparatorRegistry equalityComparatorRegistry = newEqualityComparatorRegistry(
          EqualityComparator.newInstance(Boolean.class, (context, o1, o2) -> false));

      assertFalse(TestUtil.equals(
          equalityComparatorRegistry,
          ImmutableMap.of(new Integer(1), new Boolean(true), new Integer(2), new Boolean(true)),
          ImmutableMap.of(new Integer(1), new Boolean(true), new Integer(2), new Boolean(true))));
    }
  }
}
