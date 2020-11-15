package org.triplea.modules.chat.event.processing;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.db.dao.api.key.PlayerApiKeyLookupRecord;
import org.triplea.db.dao.user.role.UserRole;
import org.triplea.domain.data.PlayerChatId;
import org.triplea.java.IpAddressParser;
import org.triplea.modules.chat.ChatterSession;
import org.triplea.web.socket.WebSocketSession;

@ExtendWith(MockitoExtension.class)
class ChatParticipantAdapterTest {

  private static final String USERNAME = "username-value";
  private final ChatParticipantAdapter chatParticipantAdapter = new ChatParticipantAdapter();

  @Mock private WebSocketSession session;

  @Test
  @DisplayName("Check data is copied from database lookup result to chatter session result object")
  void verifyData() {
    when(session.getRemoteAddress()).thenReturn(IpAddressParser.fromString("1.1.1.1"));
    final var userWithRoleRecord =
        PlayerApiKeyLookupRecord.builder()
            .userId(20)
            .apiKeyId(123)
            .username(USERNAME)
            .userRole(UserRole.PLAYER)
            .playerChatId(PlayerChatId.newId().getValue())
            .build();

    final ChatterSession result = chatParticipantAdapter.apply(session, userWithRoleRecord);

    assertThat(result.getSession(), is(session));
    assertThat(result.getApiKeyId(), is(123));
    assertThat(result.getChatParticipant().getPlayerChatId(), notNullValue());
    assertThat(result.getIp(), is(IpAddressParser.fromString("1.1.1.1")));
  }

  @ParameterizedTest
  @ValueSource(strings = {UserRole.ADMIN, UserRole.MODERATOR})
  @DisplayName("Verify moderator flag is set for moderator user roles")
  void moderatorUsers(final String moderatorUserRole) {
    final var userWithRoleRecord = givenUserRecordWithRole(moderatorUserRole, 1);
    when(session.getRemoteAddress()).thenReturn(IpAddressParser.fromString("1.1.1.1"));

    final ChatterSession result = chatParticipantAdapter.apply(session, userWithRoleRecord);

    assertThat(result.getChatParticipant().isModerator(), is(true));
    assertThat(result.getChatParticipant().getUserName().getValue(), is(USERNAME));
  }

  private PlayerApiKeyLookupRecord givenUserRecordWithRole(
      final String userRole, final int userId) {
    return PlayerApiKeyLookupRecord.builder()
        .username(USERNAME)
        .userRole(userRole)
        .playerChatId(PlayerChatId.newId().getValue())
        .apiKeyId(123)
        .userId(userId)
        .build();
  }

  @ParameterizedTest
  @CsvSource(value = {UserRole.ANONYMOUS + ",0", UserRole.PLAYER + ",1"})
  @DisplayName("Verify moderator flag is false for non-moderator roles")
  void nonModeratorUsers(final String notModeratorUserRole, final String userId) {
    final var userWithRoleRecord =
        givenUserRecordWithRole(notModeratorUserRole, Integer.parseInt(userId));
    when(session.getRemoteAddress()).thenReturn(IpAddressParser.fromString("1.1.1.1"));

    final ChatterSession result = chatParticipantAdapter.apply(session, userWithRoleRecord);

    assertThat(result.getChatParticipant().isModerator(), is(false));
    assertThat(result.getChatParticipant().getUserName().getValue(), is(USERNAME));
  }
}
