package games.strategy.engine.framework.systemcheck;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

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
    final SystemCheck check = new SystemCheck("msg", () -> throwWrappedInRuntimeException(testException));

    assertThat(check.wasSuccess(), is(false));
    assertThat(check.getResultMessage(), is("msg: false"));
  }

  @Test
  public void remembersAndReturnsExceptions() {
    final SystemCheck check = new SystemCheck("msg", () -> throwWrappedInRuntimeException(testException));
    assertThat(check.getException().isPresent(), is(true));
  }

  static void throwWrappedInRuntimeException(final Throwable throwable) {
    throw new RuntimeException(throwable);
  }
}
