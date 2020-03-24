package org.triplea.db.dao.api.key;

import com.google.common.base.Preconditions;
import javax.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.jdbi.v3.core.mapper.RowMapper;
import org.triplea.db.data.UserRole;
import org.triplea.java.Postconditions;

/** Maps ResultSet data when querying for a users API key. */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@EqualsAndHashCode
@ToString
// TODO: Project#12 rename to ApiKeyLookupRecord
public class UserWithRoleRecord {
  public static final String PLAYER_CHAT_ID_COLUMN = "player_chat_id";
  public static final String ROLE_COLUMN = "role";
  public static final String USERNAME_COLUMN = "username";
  public static final String USER_ID_COLUMN = "id";

  private @Nullable Integer userId;
  private @Nullable String username;
  private String playerChatId;
  private String role;

  /** Returns a JDBI row mapper used to convert a ResultSet into an instance of this bean object. */
  public static RowMapper<UserWithRoleRecord> buildResultMapper() {
    return (rs, ctx) -> {
      final var userWithRoleRecord =
          UserWithRoleRecord.builder()
              .userId(rs.getInt(USER_ID_COLUMN) == 0 ? null : rs.getInt(USER_ID_COLUMN))
              .playerChatId(Preconditions.checkNotNull(rs.getString(PLAYER_CHAT_ID_COLUMN)))
              .role(Preconditions.checkNotNull(rs.getString(ROLE_COLUMN)))
              .username(rs.getString(USERNAME_COLUMN))
              .build();

      Postconditions.assertState(userWithRoleRecord.role != null);

      if (userWithRoleRecord.role.equals(UserRole.HOST)) {
        Postconditions.assertState(userWithRoleRecord.username == null);
        Postconditions.assertState(userWithRoleRecord.userId == null);
      } else if (userWithRoleRecord.role.equals(UserRole.ANONYMOUS)) {
        Postconditions.assertState(userWithRoleRecord.userId == null);
        Postconditions.assertState(userWithRoleRecord.username != null);
      } else {
        Postconditions.assertState(userWithRoleRecord.userId != null);
        Postconditions.assertState(userWithRoleRecord.username != null);
      }
      return userWithRoleRecord;
    };
  }
}
