package org.triplea.test.common.matchers;

import static org.hamcrest.core.IsNot.not;

import java.util.List;
import java.util.function.Function;
import lombok.experimental.UtilityClass;
import org.hamcrest.Matcher;
import org.triplea.test.common.CustomMatcher;

@UtilityClass
public class CollectionMatchers {

  /**
   * Matcher for a collection that maps the contents of the collection and checks if any mapped
   * items match a given item.
   *
   * <p>For example, to check a list of numbers for a square that is equal to 25, the following
   * would be true: {@code assertThat(List.of(1, 3, 5), containsMappedItem(i -> i*i, 25));}
   *
   * @param mappingFunction The mapping function to apply to the collection.
   * @param mappedItem The given item to assert is contained in the mapped collection.
   */
  public static <T, X> Matcher<List<T>> containsMappedItem(
      final Function<T, X> mappingFunction, final X mappedItem) {

    return CustomMatcher.<List<T>>builder()
        .checkCondition(
            itemList -> itemList.stream().map(mappingFunction).anyMatch(mappedItem::equals))
        .description("Collection containing subelement " + mappedItem)
        .debug(Object::toString)
        .build();
  }

  public static <T, X> Matcher<List<T>> doesNotContainMappedItem(
      final Function<T, X> mappingFunction, final X mappedItem) {
    return not(containsMappedItem(mappingFunction, mappedItem));
  }
}
