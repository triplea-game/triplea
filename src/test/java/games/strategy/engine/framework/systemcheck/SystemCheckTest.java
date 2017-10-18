package games.strategy.engine.framework.systemcheck;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

public class SystemCheckTest {

  private final Exception testException = new Exception("Testing");

  @Test
  public void testPassingSystemCheck() {
    final SystemCheck check = new SystemCheck("msg", () -> {
    });

    assertThat(check.wasSuccess(), is(true));
    assertThat(check.getResultMessage(), is("msg: true"));
    assertThat(check.getException().isPresent(), is(false));
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
    assertThat(check.getException().isPresent(), is(true));
  }
}
