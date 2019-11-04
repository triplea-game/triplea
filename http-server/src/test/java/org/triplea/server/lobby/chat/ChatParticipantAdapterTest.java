package org.triplea.server.lobby.chat;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.triplea.domain.data.PlayerName;
import org.triplea.http.client.lobby.chat.ChatParticipant;
import org.triplea.lobby.server.db.data.UserRole;
import org.triplea.lobby.server.db.data.UserWithRoleRecord;

class ChatParticipantAdapterTest {

  private static final String USERNAME = "username-value";
  private final ChatParticipantAdapter chatParticipantAdapter = new ChatParticipantAdapter();

  @ParameterizedTest
  @ValueSource(strings = {UserRole.ADMIN, UserRole.MODERATOR})
  void moderatorUsers(final String moderatorUserRole) {
    final var userWithRoleRecord = givenUserRecordWithRole(moderatorUserRole);

    final ChatParticipant result = chatParticipantAdapter.apply(userWithRoleRecord);

    assertThat(
        result,
        is(
            ChatParticipant.builder()
                .isModerator(true)
                .playerName(PlayerName.of(USERNAME))
                .build()));
  }

  private UserWithRoleRecord givenUserRecordWithRole(final String userRole) {
    return UserWithRoleRecord.builder().username(USERNAME).role(userRole).build();
  }

  @ParameterizedTest
  @ValueSource(strings = {UserRole.ANONYMOUS, UserRole.PLAYER})
  void nonModeratorUsers(final String notModeratorUserRole) {
    final var userWithRoleRecord = givenUserRecordWithRole(notModeratorUserRole);

    final ChatParticipant result = chatParticipantAdapter.apply(userWithRoleRecord);

    assertThat(
        result,
        is(
            ChatParticipant.builder()
                .isModerator(true)
                .playerName(PlayerName.of(USERNAME))
                .build()));
  }
}
