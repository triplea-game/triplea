package org.triplea.http.client;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import java.util.Map;
import org.hamcrest.core.IsCollectionContaining;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.domain.data.SystemId;

@ExtendWith(MockitoExtension.class)
class SystemIdHeaderTest {
  private static final String SYSTEM_ID = "system-id";

  @Test
  void headerHasExpectedKey() {
    final Map<String, Object> headers = SystemIdHeader.headers(() -> SystemId.of(SYSTEM_ID));

    assertThat(headers.keySet(), IsCollectionContaining.hasItem(SystemIdHeader.SYSTEM_ID_HEADER));
  }

  @Test
  void headersHasExpectedValue() {
    final Map<String, Object> headers = SystemIdHeader.headers(() -> SystemId.of(SYSTEM_ID));

    assertThat(headers.get(SystemIdHeader.SYSTEM_ID_HEADER), is(SYSTEM_ID));
  }
}
