package games.strategy.util;

import static games.strategy.util.CollectionUtils.countMatches;
import static games.strategy.util.CollectionUtils.getMatches;
import static games.strategy.util.CollectionUtils.getNMatches;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

public final class CollectionUtilsTest {
  private static final Predicate<Integer> ALWAYS = it -> true;
  private static final Predicate<Integer> NEVER = it -> false;
  private static final Predicate<Integer> IS_ZERO = it -> it == 0;

  @Test
  public void testCountMatches() {
    assertEquals(0, countMatches(Arrays.asList(), IS_ZERO));

    assertEquals(1, countMatches(Arrays.asList(0), IS_ZERO));
    assertEquals(1, countMatches(Arrays.asList(-1, 0, 1), IS_ZERO));

    assertEquals(2, countMatches(Arrays.asList(0, 0), IS_ZERO));
    assertEquals(2, countMatches(Arrays.asList(-1, 0, 1, 0), IS_ZERO));
  }

  @Test
  public void testGetMatches() {
    final Collection<Integer> input = Arrays.asList(-1, 0, 1);

    assertEquals(Arrays.asList(), getMatches(Arrays.asList(), ALWAYS), "empty collection");
    assertEquals(Arrays.asList(), getMatches(input, NEVER), "none match");
    assertEquals(Arrays.asList(-1, 1), getMatches(input, IS_ZERO.negate()), "some match");
    assertEquals(Arrays.asList(-1, 0, 1), getMatches(input, ALWAYS), "all match");
  }

  @Test
  public void testGetNMatches() {
    final Collection<Integer> input = Arrays.asList(-1, 0, 1);

    assertEquals(Arrays.asList(), getNMatches(Arrays.asList(), 999, ALWAYS), "empty collection");
    assertEquals(Arrays.asList(), getNMatches(input, 0, NEVER), "max = 0");
    assertEquals(Arrays.asList(), getNMatches(input, input.size(), NEVER), "none match");
    assertEquals(Arrays.asList(0), getNMatches(Arrays.asList(-1, 0, 0, 1), 1, IS_ZERO), "some match; max < count");
    assertEquals(Arrays.asList(0, 0), getNMatches(Arrays.asList(-1, 0, 0, 1), 2, IS_ZERO), "some match; max = count");
    assertEquals(Arrays.asList(0, 0), getNMatches(Arrays.asList(-1, 0, 0, 1), 3, IS_ZERO), "some match; max > count");
    assertEquals(Arrays.asList(-1, 0), getNMatches(input, input.size() - 1, ALWAYS), "all match; max < count");
    assertEquals(Arrays.asList(-1, 0, 1), getNMatches(input, input.size(), ALWAYS), "all match; max = count");
    assertEquals(Arrays.asList(-1, 0, 1), getNMatches(input, input.size() + 1, ALWAYS), "all match; max > count");
  }

  @Test
  public void testGetNMatches_ShouldThrowExceptionWhenMaxIsNegative() {
    assertThrows(IllegalArgumentException.class, () -> getNMatches(Arrays.asList(-1, 0, 1), -1, ALWAYS));
  }
}
