package org.triplea.lobby.server.db.data;

import javax.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.jdbi.v3.core.mapper.RowMapper;
import org.triplea.java.Postconditions;

/** Maps ResultSet data when querying for a users API key. */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@EqualsAndHashCode
@ToString
public class ApiKeyUserData {

  public static final String ROLE_COLUMN = "role";
  public static final String USERNAME_COLUMN = "username";
  public static final String USER_ID_COLUMN = "id";

  private @Nullable Integer userId;
  private @Nullable String username;
  private String role;

  /** Returns a JDBI row mapper used to convert a ResultSet into an instance of this bean object. */
  // TODO: test the mapping to user role
  public static RowMapper<ApiKeyUserData> buildResultMapper() {
    return (rs, ctx) -> {
      final ApiKeyUserData userData =
          ApiKeyUserData.builder()
              .userId(rs.getInt(USER_ID_COLUMN) == 0 ? null : rs.getInt(USER_ID_COLUMN))
              .role(rs.getString(ROLE_COLUMN))
              .username(rs.getString(USERNAME_COLUMN))
              .build();

      Postconditions.assertState(
          (userData.username == null && userData.role.equals(UserRole.HOST))
              || (userData.username != null && !userData.role.equals(UserRole.HOST)),
          "Only UserRole.Host is allowed to not have a username: " + userData);

      // TODO: see if we can simplify here in postcondition
      Postconditions.assertState(
          (userData.userId == null
                  && (userData.role.equals(UserRole.HOST)
                      || userData.role.equals(UserRole.ANONYMOUS)))
              || (userData.userId != null
                  && (userData.role.equals(UserRole.ADMIN)
                      || userData.role.equals(UserRole.MODERATOR)
                      || userData.role.equals(UserRole.PLAYER))),
          "Registered user roles should have a user id: " + userData);

      return userData;
    };
  }
}
