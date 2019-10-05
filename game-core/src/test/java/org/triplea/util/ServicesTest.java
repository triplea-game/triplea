package org.triplea.util;

import static com.github.npathai.hamcrestopt.OptionalMatchers.isEmpty;
import static com.github.npathai.hamcrestopt.OptionalMatchers.isPresentAnd;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.triplea.util.Services.loadAny;
import static org.triplea.util.Services.tryLoadAny;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

final class ServicesTest {
  @Nested
  final class LoadAnyTest {
    @Test
    void shouldReturnServiceWhenServiceIsAvailable() {
      assertThat(loadAny(KnownService.class), is(instanceOf(KnownServiceImpl.class)));
    }

    @Test
    void shouldThrowExceptionWhenServiceNotAvailable() {
      final Exception e =
          assertThrows(ServiceNotAvailableException.class, () -> loadAny(UnknownService.class));
      assertThat(e.getMessage(), containsString(UnknownService.class.getName()));
    }
  }

  @Nested
  final class TryLoadAnyTest {
    @Test
    void shouldReturnServiceWhenServiceIsAvailable() {
      assertThat(
          tryLoadAny(KnownService.class), isPresentAnd(is(instanceOf(KnownServiceImpl.class))));
    }

    @Test
    void shouldReturnEmptyWhenServiceNotAvailable() {
      assertThat(tryLoadAny(UnknownService.class), isEmpty());
    }
  }

  interface KnownService {}

  public static final class KnownServiceImpl implements KnownService {}

  interface UnknownService {}
}
