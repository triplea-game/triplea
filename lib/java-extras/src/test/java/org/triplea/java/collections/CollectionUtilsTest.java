package org.triplea.java.collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.triplea.java.collections.CollectionUtils.countMatches;
import static org.triplea.java.collections.CollectionUtils.getMatches;
import static org.triplea.java.collections.CollectionUtils.getNMatches;
import static org.triplea.java.collections.CollectionUtils.haveEqualSizeAndEquivalentElements;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

final class CollectionUtilsTest {
  private static final Predicate<Integer> ALWAYS = it -> true;
  private static final Predicate<Integer> NEVER = it -> false;
  private static final Predicate<Integer> IS_ZERO = it -> it == 0;

  @Nested
  final class CountMatchesTest {
    @Test
    void shouldReturnCountOfMatches() {
      assertEquals(0, countMatches(List.of(), IS_ZERO));

      assertEquals(1, countMatches(List.of(0), IS_ZERO));
      assertEquals(1, countMatches(List.of(-1, 0, 1), IS_ZERO));

      assertEquals(2, countMatches(List.of(0, 0), IS_ZERO));
      assertEquals(2, countMatches(List.of(-1, 0, 1, 0), IS_ZERO));
    }
  }

  @Nested
  final class GetMatchesTest {
    @Test
    void shouldFilterOutNonMatchingElementsAndReturnAllMatches() {
      final Collection<Integer> input = List.of(-1, 0, 1);

      assertEquals(List.of(), getMatches(List.of(), ALWAYS), "empty collection");
      assertEquals(List.of(), getMatches(input, NEVER), "none match");
      assertEquals(List.of(-1, 1), getMatches(input, IS_ZERO.negate()), "some match");
      assertEquals(List.of(-1, 0, 1), getMatches(input, ALWAYS), "all match");
    }

    @Test
    void returnsDistinctInstanceWithDifferenceStorage() {
      final Collection<Integer> input = new ArrayList<>(List.of(-1, 0, 1));
      final List<Integer> result = getMatches(input, ALWAYS);
      assertThat(result, equalTo(input));
      assertThat(result, not(sameInstance(input)));
      // Modifying input shouldn't change result.
      input.add(5);
      assertThat(result, not(equalTo(input)));
    }

    @Test
    void returnsMutableInstance() {
      final Collection<Integer> input = List.of(-1, 0, 1);
      final List<Integer> result = getMatches(input, IS_ZERO.negate());
      assertThat(result, equalTo(List.of(-1, 1)));
      result.add(5);
      assertThat(result, equalTo(List.of(-1, 1, 5)));
    }
  }

  @Nested
  final class GetNMatchesTest {
    @Test
    void shouldFilterOutNonMatchingElementsAndReturnMaxMatches() {
      final Collection<Integer> input = List.of(-1, 0, 1);

      assertEquals(List.of(), getNMatches(List.of(), 999, ALWAYS), "empty collection");
      assertEquals(List.of(), getNMatches(input, 0, NEVER), "max = 0");
      assertEquals(List.of(), getNMatches(input, input.size(), NEVER), "none match");
      assertEquals(
          List.of(0), getNMatches(List.of(-1, 0, 0, 1), 1, IS_ZERO), "some match; max < count");
      assertEquals(
          List.of(0, 0), getNMatches(List.of(-1, 0, 0, 1), 2, IS_ZERO), "some match; max = count");
      assertEquals(
          List.of(0, 0), getNMatches(List.of(-1, 0, 0, 1), 3, IS_ZERO), "some match; max > count");
      assertEquals(
          List.of(-1, 0), getNMatches(input, input.size() - 1, ALWAYS), "all match; max < count");
      assertEquals(
          List.of(-1, 0, 1), getNMatches(input, input.size(), ALWAYS), "all match; max = count");
      assertEquals(
          List.of(-1, 0, 1),
          getNMatches(input, input.size() + 1, ALWAYS),
          "all match; max > count");
    }

    @Test
    void shouldThrowExceptionWhenMaxIsNegative() {
      assertThrows(
          IllegalArgumentException.class, () -> getNMatches(List.of(-1, 0, 1), -1, ALWAYS));
    }
  }

  @Nested
  final class HaveEqualSizeAndEquivalentElementsTest {
    @Test
    void shouldReturnTrueWhenCollectionsAreEqual() {
      assertThat(haveEqualSizeAndEquivalentElements(List.of(1, 2, 3), List.of(1, 2, 3)), is(true));
    }

    @Test
    void shouldReturnTrueWhenCollectionsAreNotEqualButHaveSameSizeAndEquivalentElements() {
      assertThat(haveEqualSizeAndEquivalentElements(List.of(1, 2, 1), List.of(2, 1, 2)), is(true));
    }

    @Test
    void shouldReturnFalseWhenCollectionsHaveEquivalentElementsButDifferentSize() {
      assertThat(haveEqualSizeAndEquivalentElements(List.of(1, 2), List.of(1, 2, 2)), is(false));
    }

    @Test
    void shouldReturnFalseWhenCollectionsHaveSameSizeButElementsAreNotEquivalent() {
      assertThat(haveEqualSizeAndEquivalentElements(List.of(1, 2, 3), List.of(1, 2, 2)), is(false));
      assertThat(haveEqualSizeAndEquivalentElements(List.of(1, 2, 2), List.of(1, 2, 3)), is(false));
    }
  }

  @Nested
  final class CreateSortedCollectionTest {
    @Test
    void supportsDuplicates() {
      final Collection<Integer> collection =
          CollectionUtils.createSortedCollection(List.of(1, 2, 3, -1, 2, 2), null);
      assertThat(collection.toArray(), is(new Integer[] {-1, 1, 2, 2, 2, 3}));
    }

    @Test
    void staysSortedWhenModified() {
      final Collection<Integer> collection =
          CollectionUtils.createSortedCollection(List.of(35, 53, 9), null);
      assertThat(collection.toArray(), is(new Integer[] {9, 35, 53}));
      collection.add(10);
      assertThat(collection.toArray(), is(new Integer[] {9, 10, 35, 53}));
      collection.remove(35);
      assertThat(collection.toArray(), is(new Integer[] {9, 10, 53}));
      collection.addAll(List.of(25, -100));
      assertThat(collection.toArray(), is(new Integer[] {-100, 9, 10, 25, 53}));
    }

    @Test
    void iterationOrder() {
      final Collection<Integer> collection =
          CollectionUtils.createSortedCollection(List.of(9, 5, 4, 12), null);
      assertThat(List.copyOf(collection), is(List.of(4, 5, 9, 12)));
    }
  }
}
