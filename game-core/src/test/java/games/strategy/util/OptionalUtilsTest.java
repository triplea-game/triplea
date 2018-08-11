package games.strategy.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

final class OptionalUtilsTest {
  @Nested
  final class IfPresentOrElseTest {
    @Test
    void shouldInvokePresentActionWhenValueIsPresent() {
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
    void shouldInvokeEmptyActionWhenValueIsAbsent() {
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
}
