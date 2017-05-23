package games.strategy.util;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

public final class OptionalUtilsTest {
  @Test
  public void ifPresentOrElse_ShouldInvokePresentActionWhenValueIsPresent() {
    final Object value = new Object();
    final AtomicBoolean presentActionInvoked = new AtomicBoolean(false);

    OptionalUtils.ifPresentOrElse(
        Optional.of(value),
        it -> {
          presentActionInvoked.set(true);
          assertThat(it, is(value));
        },
        () -> {
          fail("empty action should not have been invoked");
        });

    assertThat(presentActionInvoked.get(), is(true));
  }

  @Test
  public void ifPresentOrElse_ShouldInvokeEmptyActionWhenValueIsAbsent() {
    final AtomicBoolean emptyActionInvoked = new AtomicBoolean(false);

    OptionalUtils.ifPresentOrElse(
        Optional.empty(),
        it -> {
          fail("present action should not have been invoked");
        },
        () -> {
          emptyActionInvoked.set(true);
        });

    assertThat(emptyActionInvoked.get(), is(true));
  }
}
