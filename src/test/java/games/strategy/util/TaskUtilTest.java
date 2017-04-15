package games.strategy.util;

import static com.googlecode.catchexception.CatchException.catchException;
import static com.googlecode.catchexception.CatchException.caughtException;
import static com.googlecode.catchexception.apis.CatchExceptionHamcrestMatchers.hasNoCause;
import static com.googlecode.catchexception.throwable.CatchThrowable.catchThrowable;
import static com.googlecode.catchexception.throwable.CatchThrowable.caughtThrowable;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

/**
 * A fixture for testing the basic aspects of the {@link TaskUtil} class.
 */
public final class TaskUtilTest {
  @Test
  public void launderThrowable_ShouldReturnCauseWhenCauseIsUncheckedException() {
    final Throwable result = TaskUtil.launderThrowable(new UnsupportedOperationException());

    assertThat(result, is(instanceOf(UnsupportedOperationException.class)));
  }

  @Test
  public void launderThrowable_ShouldThrowErrorWhenCauseIsError() {
    catchThrowable(() -> TaskUtil.launderThrowable(new AssertionError()));

    assertThat(caughtThrowable(), is(instanceOf(AssertionError.class)));
  }

  @Test
  public void launderThrowable_ShouldThrowIllegalStateExceptionWhenCauseIsCheckedException() {
    catchException(() -> TaskUtil.launderThrowable(new InstantiationException()));

    assertThat(caughtException(), is(instanceOf(IllegalStateException.class)));
    assertThat(caughtException().getCause(), is(instanceOf(InstantiationException.class)));
  }

  @Test
  public void launderThrowable_ShouldThrowIllegalStateExceptionWhenCauseIsNull() {
    catchException(() -> TaskUtil.launderThrowable(null));

    assertThat(caughtException(), allOf(is(instanceOf(IllegalStateException.class)), hasNoCause()));
  }
}
