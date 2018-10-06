package games.strategy.engine.auto.health.check;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.triplea.test.common.assertions.Optionals.isMissing;
import static org.triplea.test.common.assertions.Optionals.isPresent;

import org.junit.jupiter.api.Test;

public class SystemCheckTest {

  private final Exception testException = new Exception("Testing");

  @Test
  public void testPassingSystemCheck() {
    final SystemCheck check = new SystemCheck("msg", () -> {
    });

    assertThat(check.wasSuccess(), is(true));
    assertThat(check.getResultMessage(), is("msg: true"));
    assertThat(check.getException(), isMissing());
  }

  @Test
  public void testFailingSystemCheck() {
    final SystemCheck check = new SystemCheck("msg", () -> {
      throw new RuntimeException(testException);
    });

    assertThat(check.wasSuccess(), is(false));
    assertThat(check.getResultMessage(), is("msg: false"));
  }

  @Test
  public void remembersAndReturnsExceptions() {
    final SystemCheck check = new SystemCheck("msg", () -> {
      throw new RuntimeException(testException);
    });
    assertThat(check.getException(), isPresent());
  }
}
