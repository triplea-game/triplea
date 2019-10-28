package org.triplea.http.client;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.http.client.SystemIdHeader.PreferencesPersistence;

@ExtendWith(MockitoExtension.class)
class SystemIdHeaderTest {
  private static final String SYSTEM_ID = "system-id";

  @Mock private PreferencesPersistence preferencesPersistence;

  @BeforeEach
  void setup() {
    SystemIdHeader.setPreferencesPersistence(preferencesPersistence);
  }

  @Test
  void headersHasExpectedKey() {
    when(preferencesPersistence.get()).thenReturn(SYSTEM_ID);

    final Map<String, Object> headers = SystemIdHeader.headers();

    assertThat(headers.keySet(), hasItem(SystemIdHeader.SYSTEM_ID_HEADER));
  }

  @Test
  void headersWithValueAlreadyExisting() {
    when(preferencesPersistence.get()).thenReturn(SYSTEM_ID);

    final Map<String, Object> headers = SystemIdHeader.headers();

    assertThat(headers.get(SystemIdHeader.SYSTEM_ID_HEADER), is(SYSTEM_ID));
  }

  @Test
  void headersWithValueNotYetExisting() {
    when(preferencesPersistence.get()).thenReturn(null);

    final Map<String, Object> headers = SystemIdHeader.headers();

    assertThat(headers.get(SystemIdHeader.SYSTEM_ID_HEADER), notNullValue());
    verify(preferencesPersistence).save(anyString());
  }
}
