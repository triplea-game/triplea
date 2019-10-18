package org.triplea.lobby.server.db.data;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ApiKeyUserDataTest {
  private static final String ROLE = UserRole.PLAYER;
  private static final int USER_ID = 55;
  private static final String NAME = "player-name";

  @Mock private ResultSet resultSet;

  @Test
  void buildResultMapper() throws Exception {
    givenResults(USER_ID, ROLE, NAME);

    final ApiKeyUserData result = ApiKeyUserData.buildResultMapper().map(resultSet, null);

    assertThat(result.getUserId(), is(USER_ID));
    assertThat(result.getRole(), is(UserRole.PLAYER));
    assertThat(result.getUsername(), is(NAME));
  }

  private void givenResults(final int userId, final String role, final String name)
      throws Exception {
    when(resultSet.getInt(ApiKeyUserData.USER_ID_COLUMN)).thenReturn(userId);
    when(resultSet.getString(ApiKeyUserData.ROLE_COLUMN)).thenReturn(role);
    when(resultSet.getString(ApiKeyUserData.USERNAME_COLUMN)).thenReturn(name);
  }

  @Test
  void zeroUserIdIsMappedToNull() throws Exception {
    givenResults(0, UserRole.HOST, null);

    final ApiKeyUserData result = ApiKeyUserData.buildResultMapper().map(resultSet, null);

    assertThat(result.getUserId(), nullValue());
  }

  @ParameterizedTest
  @ValueSource(strings = {UserRole.PLAYER, UserRole.MODERATOR, UserRole.MODERATOR})
  void assertInvalidStatesRolesThatMustHaveUserId(final String userRole) throws Exception {
    givenResults(0, userRole, NAME);

    assertPostconditionFailure();
  }

  void assertPostconditionFailure() {
    assertThrows(
        AssertionError.class, () -> ApiKeyUserData.buildResultMapper().map(resultSet, null));
  }

  @ParameterizedTest
  @ValueSource(strings = {UserRole.ANONYMOUS, UserRole.HOST})
  void assertInvalidStatesRolesThatMustNotHaveUserId(final String userRole) throws Exception {
    givenResults(USER_ID, userRole, NAME);

    assertPostconditionFailure();
  }

  @SuppressWarnings("unused")
  private static List<Arguments> mustHaveUserName() {
    return Arrays.asList(
        Arguments.of(0, UserRole.ANONYMOUS), //
        Arguments.of(USER_ID, UserRole.PLAYER), //
        Arguments.of(USER_ID, UserRole.MODERATOR), //
        Arguments.of(USER_ID, UserRole.ADMIN));
  }

  @ParameterizedTest
  @MethodSource("mustHaveUserName")
  void assertInvalidStatesRolesThatMustHaveName(final int userId, final String roleName)
      throws Exception {
    givenResults(userId, roleName, null);

    assertPostconditionFailure();
  }

  @Test
  void assertInvalidStatesHostRoleMayNotHaveName() throws Exception {
    givenResults(0, UserRole.HOST, NAME);

    assertPostconditionFailure();
  }
}
