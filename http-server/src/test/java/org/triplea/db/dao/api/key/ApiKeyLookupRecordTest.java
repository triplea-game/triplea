package org.triplea.db.dao.api.key;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.db.data.UserRole;

@ExtendWith(MockitoExtension.class)
class ApiKeyLookupRecordTest {
  private static final String ROLE = UserRole.PLAYER;
  private static final int USER_ID = 55;
  private static final String NAME = "player-name";
  private static final String CHAT_ID = "chat-id";

  @Mock private ResultSet resultSet;

  @Test
  void buildResultMapper() throws Exception {
    givenResults(USER_ID, CHAT_ID, ROLE, NAME);

    final ApiKeyLookupRecord result = ApiKeyLookupRecord.buildResultMapper().map(resultSet, null);

    assertThat(result.getUserId(), is(USER_ID));
    assertThat(result.getRole(), is(UserRole.PLAYER));
    assertThat(result.getUsername(), is(NAME));
    assertThat(result.getPlayerChatId(), is(CHAT_ID));
  }

  @SuppressWarnings("SameParameterValue")
  private void givenResults(
      final int userId, final String chatId, final String role, final String name)
      throws Exception {
    when(resultSet.getInt(ApiKeyLookupRecord.USER_ID_COLUMN)).thenReturn(userId);
    when(resultSet.getString(ApiKeyLookupRecord.PLAYER_CHAT_ID_COLUMN)).thenReturn(chatId);
    when(resultSet.getString(ApiKeyLookupRecord.ROLE_COLUMN)).thenReturn(role);
    when(resultSet.getString(ApiKeyLookupRecord.USERNAME_COLUMN)).thenReturn(name);
  }

  @Test
  void zeroUserIdIsMappedToNull() throws Exception {
    givenResults(0, CHAT_ID, UserRole.HOST, null);

    final ApiKeyLookupRecord result = ApiKeyLookupRecord.buildResultMapper().map(resultSet, null);

    assertThat(result.getUserId(), nullValue());
  }

  @ParameterizedTest
  @ValueSource(strings = {UserRole.PLAYER, UserRole.MODERATOR, UserRole.MODERATOR})
  void assertInvalidStatesRolesThatMustHaveUserId(final String userRole) throws Exception {
    givenResults(0, CHAT_ID, userRole, NAME);

    assertPostconditionFailure();
  }

  void assertPostconditionFailure() {
    assertThrows(
        AssertionError.class, () -> ApiKeyLookupRecord.buildResultMapper().map(resultSet, null));
  }

  @ParameterizedTest
  @ValueSource(strings = {UserRole.ANONYMOUS, UserRole.HOST})
  void assertInvalidStatesRolesThatMustNotHaveUserId(final String userRole) throws Exception {
    givenResults(USER_ID, CHAT_ID, userRole, NAME);

    assertPostconditionFailure();
  }

  @SuppressWarnings("unused")
  private static List<Arguments> mustHaveUserName() {
    return List.of(
        Arguments.of(0, UserRole.ANONYMOUS), //
        Arguments.of(USER_ID, UserRole.PLAYER), //
        Arguments.of(USER_ID, UserRole.MODERATOR), //
        Arguments.of(USER_ID, UserRole.ADMIN));
  }

  @ParameterizedTest
  @MethodSource("mustHaveUserName")
  void assertInvalidStatesRolesThatMustHaveName(final int userId, final String roleName)
      throws Exception {
    givenResults(userId, CHAT_ID, roleName, null);

    assertPostconditionFailure();
  }

  @Test
  void assertInvalidStatesHostRoleMayNotHaveName() throws Exception {
    givenResults(0, CHAT_ID, UserRole.HOST, NAME);

    assertPostconditionFailure();
  }
}
