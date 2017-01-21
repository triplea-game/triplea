package games.strategy.engine.framework.systemcheck;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableSet;

public class LocalSystemCheckerTest {

  private final static SystemCheck PASSING_CHECK = new SystemCheck("no op", () -> {
  });
  private final static SystemCheck FAILING_CHECK =
      new SystemCheck("throws exception", () -> Throwables.propagate(new Exception("test")));

  @Test
  public void testHappyCase() {
    final LocalSystemChecker checker = new LocalSystemChecker(ImmutableSet.of(PASSING_CHECK));
    assertThat(checker.getExceptions().size(), is(0));
  }

  @Test
  public void testCheckingNetwork() {
    final SystemCheck network = new SystemCheck("throws exception", () -> Throwables.propagate(new Exception("test")));

    final LocalSystemChecker checker = new LocalSystemChecker(ImmutableSet.of(network));
    assertThat(checker.getExceptions().size(), is(1));
  }

  @Test
  public void testMixedCase() {
    final LocalSystemChecker checker = new LocalSystemChecker(ImmutableSet.of(PASSING_CHECK, FAILING_CHECK));
    assertThat(checker.getExceptions().size(), is(1));
  }
}
