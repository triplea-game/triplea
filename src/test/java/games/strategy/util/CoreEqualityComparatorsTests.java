package games.strategy.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import com.google.common.collect.ImmutableMap;

import games.strategy.test.EqualityComparator;
import games.strategy.test.EqualityComparatorRegistry;
import games.strategy.test.TestUtil;

@RunWith(Enclosed.class)
public final class CoreEqualityComparatorsTests {
  private static EqualityComparatorRegistry newEqualityComparatorRegistryOf(
      final EqualityComparator primaryEqualityComparator,
      final EqualityComparator... secondaryEqualityComparators) {
    final Collection<EqualityComparator> equalityComparators = new ArrayList<>();
    equalityComparators.add(primaryEqualityComparator);
    equalityComparators.addAll(Arrays.asList(secondaryEqualityComparators));
    return EqualityComparatorRegistry.newInstance(equalityComparators);
  }

  public static final class CollectionTest {
    private final EqualityComparatorRegistry equalityComparatorRegistry = newEqualityComparatorRegistry();

    private static EqualityComparatorRegistry newEqualityComparatorRegistry(
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

  public static final class MapTest {
    private final EqualityComparatorRegistry equalityComparatorRegistry = newEqualityComparatorRegistry();

    private static EqualityComparatorRegistry newEqualityComparatorRegistry(
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
