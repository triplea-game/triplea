package org.triplea.db.dao.user.role;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jdbi.v3.core.mapper.RowMapper;
import org.triplea.java.Postconditions;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@EqualsAndHashCode
public class UserRoleLookup {
  private int userId;
  private int userRoleId;

  /** Returns a JDBI row mapper used to convert results into an instance of this bean object. */
  public static RowMapper<UserRoleLookup> buildResultMapper() {
    return (rs, ctx) -> {
      final UserRoleLookup roleLookup =
          UserRoleLookup.builder()
              .userId(rs.getInt("id"))
              .userRoleId(rs.getInt("user_role_id"))
              .build();
      Postconditions.assertState(roleLookup.userId != 0);
      Postconditions.assertState(roleLookup.userRoleId != 0);
      return roleLookup;
    };
  }
}
