package org.triplea.java;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

final class OptionalUtilsTest {
  @ExtendWith(MockitoExtension.class)
  @Nested
  final class IfAllPresentTest {
    private static final String FIRST_VALUE = "value";
    private static final int SECOND_VALUE = 42;

    @Mock private BiConsumer<String, Integer> action;

    @Test
    void shouldInvokeActionWhenOptional1PresentAndOptional2Present() {
      OptionalUtils.ifAllPresent(Optional.of(FIRST_VALUE), Optional.of(SECOND_VALUE), action);

      verify(action).accept(FIRST_VALUE, SECOND_VALUE);
    }

    @Test
    void shouldNotInvokeActionWhenOptional1PresentAndOptional2Absent() {
      OptionalUtils.ifAllPresent(Optional.of(FIRST_VALUE), Optional.empty(), action);

      verify(action, never()).accept(any(), any());
    }

    @Test
    void shouldNotInvokeActionWhenOptional1AbsentAndOptional2Present() {
      OptionalUtils.ifAllPresent(Optional.empty(), Optional.of(SECOND_VALUE), action);

      verify(action, never()).accept(any(), any());
    }

    @Test
    void shouldNotInvokeActionWhenOptional1AbsentAndOptional2Absent() {
      OptionalUtils.ifAllPresent(Optional.empty(), Optional.empty(), action);

      verify(action, never()).accept(any(), any());
    }
  }

  @ExtendWith(MockitoExtension.class)
  @Nested
  final class IfEmptyTest {
    @Mock private Runnable action;

    @Test
    void shouldNotInvokeActionWhenValuePresent() {
      OptionalUtils.ifEmpty(Optional.of(42), action);

      verify(action, never()).run();
    }

    @Test
    void shouldInvokeActionWhenValueAbsent() {
      OptionalUtils.ifEmpty(Optional.empty(), action);

      verify(action).run();
    }
  }

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
