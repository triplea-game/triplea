package org.triplea.server.lobby.chat;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.triplea.domain.data.PlayerName;
import org.triplea.http.client.lobby.chat.ChatParticipant;
import org.triplea.lobby.server.db.data.ApiKeyUserData;
import org.triplea.lobby.server.db.data.UserRole;

class ChatParticipantAdapterTest {

  private static final String USERNAME = "username-value";
  private final ChatParticipantAdapter chatParticipantAdapter = new ChatParticipantAdapter();

  @ParameterizedTest
  @ValueSource(strings = {UserRole.ADMIN, UserRole.MODERATOR})
  void moderatorUsers(final String moderatorUserRole) {
    final ApiKeyUserData apiKeyUserData = givenUserDataWithRole(moderatorUserRole);

    final ChatParticipant result = chatParticipantAdapter.apply(apiKeyUserData);

    assertThat(
        result,
        is(
            ChatParticipant.builder()
                .isModerator(true)
                .playerName(PlayerName.of(USERNAME))
                .build()));
  }

  private ApiKeyUserData givenUserDataWithRole(final String userRole) {
    return ApiKeyUserData.builder().username(USERNAME).role(userRole).build();
  }

  @ParameterizedTest
  @ValueSource(strings = {UserRole.ANONYMOUS, UserRole.PLAYER})
  void nonModeratorUsers(final String notModeratorUserRole) {
    final ApiKeyUserData apiKeyUserData = givenUserDataWithRole(notModeratorUserRole);

    final ChatParticipant result = chatParticipantAdapter.apply(apiKeyUserData);

    assertThat(
        result,
        is(
            ChatParticipant.builder()
                .isModerator(true)
                .playerName(PlayerName.of(USERNAME))
                .build()));
  }
}
