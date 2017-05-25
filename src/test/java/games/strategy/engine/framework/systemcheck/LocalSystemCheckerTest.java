package games.strategy.engine.framework.systemcheck;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableSet;

public class LocalSystemCheckerTest {

  private static final SystemCheck PASSING_CHECK = new SystemCheck("no op", () -> {
  });
  private static final SystemCheck FAILING_CHECK =
      new SystemCheck("throws exception", () -> Throwables.propagate(new Exception("test")));

  @Test
  public void testPassingCase() {
    final LocalSystemChecker checker = new LocalSystemChecker(ImmutableSet.of(PASSING_CHECK));
    assertThat(checker.getExceptions().size(), is(0));
  }

  @Test
  public void testFailingCase() {
    final LocalSystemChecker checker = new LocalSystemChecker(ImmutableSet.of(FAILING_CHECK));
    assertThat(checker.getExceptions().size(), is(1));
  }

  @Test
  public void testMixedCase() {
    final LocalSystemChecker checker = new LocalSystemChecker(ImmutableSet.of(PASSING_CHECK, FAILING_CHECK));
    assertThat(checker.getExceptions().size(), is(1));
  }
}
