package org.triplea.java;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Optional;
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
}
