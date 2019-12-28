package org.triplea.server.lobby.chat;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.triplea.domain.data.PlayerChatId;
import org.triplea.domain.data.UserName;
import org.triplea.http.client.lobby.chat.ChatParticipant;
import org.triplea.lobby.server.db.dao.api.key.UserWithRoleRecord;
import org.triplea.lobby.server.db.data.UserRole;

class ChatParticipantAdapterTest {

  private static final String USERNAME = "username-value";
  private final ChatParticipantAdapter chatParticipantAdapter = new ChatParticipantAdapter();

  @Test
  @DisplayName("Verify player chat Id will be generated")
  void playerChatIdIsGenerated() {
    final var userWithRoleRecord =
        UserWithRoleRecord.builder()
            .username(USERNAME)
            .role(UserRole.PLAYER)
            .playerChatId(PlayerChatId.newId().getValue())
            .build();

    final ChatParticipant result = chatParticipantAdapter.apply(userWithRoleRecord);

    assertThat(result.getPlayerChatId(), notNullValue());
  }

  @ParameterizedTest
  @ValueSource(strings = {UserRole.ADMIN, UserRole.MODERATOR})
  @DisplayName("Verify moderator flag is set for moderator user roles")
  void moderatorUsers(final String moderatorUserRole) {
    final var userWithRoleRecord = givenUserRecordWithRole(moderatorUserRole);

    final ChatParticipant result = chatParticipantAdapter.apply(userWithRoleRecord);

    assertThat(
        result,
        is(ChatParticipant.builder().isModerator(true).userName(UserName.of(USERNAME)).build()));
  }

  private UserWithRoleRecord givenUserRecordWithRole(final String userRole) {
    return UserWithRoleRecord.builder()
        .username(USERNAME)
        .role(userRole)
        .playerChatId(PlayerChatId.newId().getValue())
        .build();
  }

  @ParameterizedTest
  @ValueSource(strings = {UserRole.ANONYMOUS, UserRole.PLAYER})
  @DisplayName("Verify moderator flag is false for non-moderator roles")
  void nonModeratorUsers(final String notModeratorUserRole) {
    final var userWithRoleRecord = givenUserRecordWithRole(notModeratorUserRole);

    final ChatParticipant result = chatParticipantAdapter.apply(userWithRoleRecord);

    assertThat(
        result,
        is(ChatParticipant.builder().isModerator(true).userName(UserName.of(USERNAME)).build()));
  }
}
