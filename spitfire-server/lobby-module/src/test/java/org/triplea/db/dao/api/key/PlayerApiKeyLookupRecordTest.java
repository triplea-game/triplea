package org.triplea.db.dao.api.key;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.triplea.db.dao.user.role.UserRole;

class PlayerApiKeyLookupRecordTest {
  private static final PlayerApiKeyLookupRecord API_KEY_LOOKUP_RECORD =
      PlayerApiKeyLookupRecord.builder()
          .userId(1)
          .apiKeyId(1)
          .playerChatId("chat-id")
          .userRole("role")
          .username("user-name")
          .build();

  @Test
  void zeroUserIdIsMappedToNull() {
    final PlayerApiKeyLookupRecord result =
        API_KEY_LOOKUP_RECORD.toBuilder().userRole(UserRole.ANONYMOUS).userId(0).build();

    assertThat(result.getUserId(), nullValue());
  }

  @ParameterizedTest
  @MethodSource
  void invalidStates(final Supplier<PlayerApiKeyLookupRecord> apiKeyLookupRecord) {
    assertThrows(AssertionError.class, apiKeyLookupRecord::get);
  }

  @SuppressWarnings("unused")
  static List<Supplier<PlayerApiKeyLookupRecord>> invalidStates() {
    return List.of(
        // Non-Anonymous roles must have a user-id
        () -> API_KEY_LOOKUP_RECORD.toBuilder().userId(0).userRole(UserRole.PLAYER).build(),
        () -> API_KEY_LOOKUP_RECORD.toBuilder().userId(0).userRole(UserRole.MODERATOR).build(),
        () -> API_KEY_LOOKUP_RECORD.toBuilder().userId(0).userRole(UserRole.ADMIN).build(),

        // Anonymous role may not have a user-id
        () -> API_KEY_LOOKUP_RECORD.toBuilder().userId(1).userRole(UserRole.ANONYMOUS).build(),

        // Host role is not allowed (host API-keys are stored in a different table)
        () -> API_KEY_LOOKUP_RECORD.toBuilder().userRole(UserRole.HOST).build());
  }
}
