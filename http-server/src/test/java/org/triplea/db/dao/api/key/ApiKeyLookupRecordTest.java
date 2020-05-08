package org.triplea.db.dao.api.key;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.triplea.db.dao.user.role.UserRole;

class ApiKeyLookupRecordTest {
  private static final ApiKeyLookupRecord API_KEY_LOOKUP_RECORD =
      ApiKeyLookupRecord.builder()
          .userId(1)
          .apiKeyId(1)
          .playerChatId("chat-id")
          .role("role")
          .username("user-name")
          .build();

  @Test
  void zeroUserIdIsMappedToNull() {
    final ApiKeyLookupRecord result = API_KEY_LOOKUP_RECORD.toBuilder().apiKeyId(0).build();

    assertThat(result.getUserId(), nullValue());
  }

  @ParameterizedTest
  @MethodSource
  void invalidStates(final ApiKeyLookupRecord apiKeyLookupRecord) {
    assertThrows(AssertionError.class, () -> ApiKeyLookupRecord.verifyState(apiKeyLookupRecord));
  }

  static List<ApiKeyLookupRecord> invalidStates() {
    return List.of(
        // Non-Anonymous roles must have a user-id
        API_KEY_LOOKUP_RECORD.toBuilder().userId(0).role(UserRole.PLAYER).build(),
        API_KEY_LOOKUP_RECORD.toBuilder().userId(0).role(UserRole.MODERATOR).build(),
        API_KEY_LOOKUP_RECORD.toBuilder().userId(0).role(UserRole.ADMIN).build(),

        // Anonymous role may not have a user-id
        API_KEY_LOOKUP_RECORD.toBuilder().userId(1).role(UserRole.ANONYMOUS).build(),

        // Host role is not allowed (host API-keys are stored in a different table)
        API_KEY_LOOKUP_RECORD.toBuilder().role(UserRole.HOST).build());
  }
}
