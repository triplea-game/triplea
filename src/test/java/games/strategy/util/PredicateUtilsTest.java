package games.strategy.util;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public final class PredicateUtilsTest {
  @Test
  public void not_ShouldReturnLogicalNegationOfPredicate() {
    final Object t = new Object();

    assertThat(PredicateUtils.not(it -> false).test(t), is(true));
    assertThat(PredicateUtils.not(it -> true).test(t), is(false));
  }
}
