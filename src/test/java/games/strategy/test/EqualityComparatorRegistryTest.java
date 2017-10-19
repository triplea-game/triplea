package games.strategy.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.jupiter.api.Test;

public final class EqualityComparatorRegistryTest {
  @Test
  public void getEqualityComparatorFor_ShouldReturnRegisteredComparatorWhenClassTypeRegistered() {
    final EqualityComparatorRegistry equalityComparatorRegistry = EqualityComparatorRegistry.newInstance(
        EqualityComparator.newInstance(ArrayList.class, (context, o1, o2) -> true));

    final EqualityComparator actual = equalityComparatorRegistry.getEqualityComparatorFor(ArrayList.class);

    assertThat(actual.getType(), is(ArrayList.class));
  }

  @Test
  public void getEqualityComparatorFor_ShouldReturnRegisteredComparatorWhenInterfaceTypeRegistered() {
    final EqualityComparatorRegistry equalityComparatorRegistry = EqualityComparatorRegistry.newInstance(
        EqualityComparator.newInstance(Collection.class, (context, o1, o2) -> true));

    final EqualityComparator actual = equalityComparatorRegistry.getEqualityComparatorFor(ArrayList.class);

    assertThat(actual.getType(), is(Collection.class));
  }

  @Test
  public void getEqualityComparatorFor_ShouldReturnDefaultComparatorWhenNoTypesRegistered() {
    final EqualityComparatorRegistry equalityComparatorRegistry = EqualityComparatorRegistry.newInstance();

    final EqualityComparator actual = equalityComparatorRegistry.getEqualityComparatorFor(ArrayList.class);

    assertThat(actual, is(not(nullValue())));
  }
}
