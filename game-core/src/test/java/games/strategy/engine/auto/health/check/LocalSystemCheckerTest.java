package games.strategy.engine.auto.health.check;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.Runnables;

public class LocalSystemCheckerTest {

  private static final SystemCheck PASSING_CHECK = new SystemCheck("no op", Runnables.doNothing());
  private static final SystemCheck FAILING_CHECK =
      new SystemCheck(
          "throws exception",
          () -> {
            throw new RuntimeException(new Exception("test"));
          });

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
    final LocalSystemChecker checker =
        new LocalSystemChecker(ImmutableSet.of(PASSING_CHECK, FAILING_CHECK));
    assertThat(checker.getExceptions().size(), is(1));
  }
}
