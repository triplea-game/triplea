package org.triplea.util;

import static org.assertj.core.api.Assertions.assertThat;
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
      assertThat(loadAny(KnownService.class)).isInstanceOf(KnownServiceImpl.class);
    }

    @Test
    void shouldThrowExceptionWhenServiceNotAvailable() {
      final Exception e =
          assertThrows(ServiceNotAvailableException.class, () -> loadAny(UnknownService.class));
      assertThat(e.getMessage()).contains(UnknownService.class.getName());
    }
  }

  @Nested
  final class TryLoadAnyTest {
    @Test
    void shouldReturnServiceWhenServiceIsAvailable() {
      assertThat(tryLoadAny(KnownService.class)).get().isInstanceOf(KnownServiceImpl.class);
    }

    @Test
    void shouldReturnEmptyWhenServiceNotAvailable() {
      assertThat(tryLoadAny(UnknownService.class)).isEmpty();
    }
  }

  interface KnownService {}

  public static final class KnownServiceImpl implements KnownService {}

  interface UnknownService {}
}
