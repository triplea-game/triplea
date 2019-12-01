package org.triplea.http.client;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsCollectionContaining.hasItem;

import java.util.Map;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.domain.data.SystemId;

@ExtendWith(MockitoExtension.class)
class SystemIdHeaderTest {
  private static final String SYSTEM_ID = "system-id";
  private static final Supplier<SystemId> SYSTEM_ID_SUPPLIER = () -> SystemId.of(SYSTEM_ID);

  @Test
  void headerHasExpectedKey() {
    final Map<String, Object> headers = SystemIdHeader.headers(SYSTEM_ID_SUPPLIER);

    assertThat(headers.keySet(), hasItem(SystemIdHeader.SYSTEM_ID_HEADER));
  }

  @Test
  void headersHasExpectedValue() {
    final Map<String, Object> headers = SystemIdHeader.headers(SYSTEM_ID_SUPPLIER);

    assertThat(headers.get(SystemIdHeader.SYSTEM_ID_HEADER), is(SYSTEM_ID));
  }
}
