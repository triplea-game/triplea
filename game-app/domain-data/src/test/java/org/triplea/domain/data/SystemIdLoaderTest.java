package org.triplea.domain.data;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.domain.data.SystemIdLoader.PreferencesPersistence;

@ExtendWith(MockitoExtension.class)
class SystemIdLoaderTest {
  private static final SystemId SYSTEM_ID = SystemId.of("system-id");

  @Mock private PreferencesPersistence preferencesPersistence;

  @BeforeEach
  void setUp() {
    SystemIdLoader.setPreferencesPersistence(preferencesPersistence);
  }

  @Test
  @DisplayName("Case: Fetching a SystemId from persistence")
  void headersWithValueAlreadyExisting() {
    when(preferencesPersistence.get()).thenReturn(SYSTEM_ID.getValue());

    final SystemId result = SystemIdLoader.load();

    assertThat(result, is(SYSTEM_ID));
  }

  @Test
  @DisplayName("Case: No SystemId in persistence, expect one to be generated and stored")
  void headersWithValueNotYetExisting() {
    when(preferencesPersistence.get()).thenReturn(null);

    final SystemId result = SystemIdLoader.load();

    assertThat(result, is(notNullValue()));
    verify(preferencesPersistence).save(anyString());
  }
}
