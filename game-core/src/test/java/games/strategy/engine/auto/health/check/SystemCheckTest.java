package games.strategy.engine.auto.health.check;

import static com.github.npathai.hamcrestopt.OptionalMatchers.isEmpty;
import static com.github.npathai.hamcrestopt.OptionalMatchers.isPresent;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import com.google.common.util.concurrent.Runnables;

public class SystemCheckTest {

  private final Exception testException = new Exception("Testing");

  @Test
  public void testPassingSystemCheck() {
    final SystemCheck check = new SystemCheck("msg", Runnables.doNothing());

    assertThat(check.wasSuccess(), is(true));
    assertThat(check.getResultMessage(), is("msg: true"));
    assertThat(check.getException(), isEmpty());
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
