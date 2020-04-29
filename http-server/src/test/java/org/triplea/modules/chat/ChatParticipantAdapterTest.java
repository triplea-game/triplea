package org.triplea.modules.chat;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;

import javax.websocket.Session;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.db.dao.api.key.ApiKeyLookupRecord;
import org.triplea.db.dao.user.role.UserRole;
import org.triplea.domain.data.PlayerChatId;
import org.triplea.modules.chat.Chatters.ChatterSession;

@ExtendWith(MockitoExtension.class)
class ChatParticipantAdapterTest {

  private static final String USERNAME = "username-value";
  private final ChatParticipantAdapter chatParticipantAdapter = new ChatParticipantAdapter();

  @Mock private Session session;

  @Test
  @DisplayName("Check data is copied from database lookup result to chatter session result object")
  void verifyData() {
    final var userWithRoleRecord =
        ApiKeyLookupRecord.builder()
            .apiKeyId(123)
            .username(USERNAME)
            .role(UserRole.PLAYER)
            .playerChatId(PlayerChatId.newId().getValue())
            .build();

    final ChatterSession result = chatParticipantAdapter.apply(session, userWithRoleRecord);

    assertThat(result.getSession(), is(session));
    assertThat(result.getApiKeyId(), is(123));
    assertThat(result.getChatParticipant().getPlayerChatId(), notNullValue());
  }

  @ParameterizedTest
  @ValueSource(strings = {UserRole.ADMIN, UserRole.MODERATOR})
  @DisplayName("Verify moderator flag is set for moderator user roles")
  void moderatorUsers(final String moderatorUserRole) {
    final var userWithRoleRecord = givenUserRecordWithRole(moderatorUserRole);

    final ChatterSession result = chatParticipantAdapter.apply(session, userWithRoleRecord);

    assertThat(result.getChatParticipant().isModerator(), is(true));
    assertThat(result.getChatParticipant().getUserName().getValue(), is(USERNAME));
  }

  private ApiKeyLookupRecord givenUserRecordWithRole(final String userRole) {
    return ApiKeyLookupRecord.builder()
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

    final ChatterSession result = chatParticipantAdapter.apply(session, userWithRoleRecord);

    assertThat(result.getChatParticipant().isModerator(), is(false));
    assertThat(result.getChatParticipant().getUserName().getValue(), is(USERNAME));
  }
}
