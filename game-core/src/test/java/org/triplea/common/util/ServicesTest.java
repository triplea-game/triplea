package org.triplea.common.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.triplea.common.util.Services.loadAny;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public final class ServicesTest {
  @Nested
  final class LoadAnyTest {
    @Test
    void shouldReturnServiceWhenServiceIsAvailable() {
      assertThat(loadAny(KnownService.class), is(instanceOf(KnownServiceImpl.class)));
    }

    @Test
    void shouldThrowExceptionWhenServiceNotAvailable() {
      final Exception e = assertThrows(ServiceNotAvailableException.class, () -> loadAny(UnknownService.class));
      assertThat(e.getMessage(), containsString(UnknownService.class.getName()));
    }
  }

  public interface KnownService {
  }

  public static final class KnownServiceImpl implements KnownService {
  }

  public interface UnknownService {
  }
}
