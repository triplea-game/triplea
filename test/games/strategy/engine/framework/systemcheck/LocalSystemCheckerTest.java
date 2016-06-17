package games.strategy.engine.framework.systemcheck;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableSet;

public class LocalSystemCheckerTest {

  private final static SystemCheck PASSING_CHECK = new SystemCheck("no op", () -> {});

  private final static SystemCheck FAILING_CHECK = new SystemCheck("throws exception", () -> Throwables.propagate(new Exception("test")));


  @Test
  public void testHappyCase() {
    LocalSystemChecker checker = new LocalSystemChecker(ImmutableSet.of(PASSING_CHECK));
    assertThat(checker.getExceptions().size(), is(0));
  }

  @Test
  public void testCheckingNetwork() {

    SystemCheck network = new SystemCheck("throws exception", () -> Throwables.propagate(new Exception("test")));

    LocalSystemChecker checker = new LocalSystemChecker(ImmutableSet.of(network));
    assertThat(checker.getExceptions().size(), is(1));
  }

  @Test
  public void testMixedCase() {
    LocalSystemChecker checker = new LocalSystemChecker(ImmutableSet.of(PASSING_CHECK, FAILING_CHECK));
    assertEquals(1, checker.getExceptions().size());
  }

}
