package games.strategy.engine.framework.systemcheck;


import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.google.common.base.Throwables;

public class SystemCheckTest {

  private final Exception testException = new Exception("Testing");

  @Test
  public void testPassingSystemCheck() {
    Runnable noOp = () -> {};
    SystemCheck check = new SystemCheck("msg", noOp);

    assertThat(check.wasSuccess(), is(true));
    assertThat(check.getResultMessage(), is("msg: true"));
    assertThat(check.getException().isPresent(), is(false));
  }


  @Test
  public void testFailingSystemCheck() {
    SystemCheck check = new SystemCheck("msg", () -> Throwables.propagate(testException));

    assertFalse(check.wasSuccess());
    assertThat(check.getResultMessage(), is("msg: false"));
  }


  @Test
  public void remembersAndReturnsExceptions() {
    SystemCheck check = new SystemCheck("msg", () -> Throwables.propagate(testException));
    assertTrue(check.getException().isPresent());
  }
}
