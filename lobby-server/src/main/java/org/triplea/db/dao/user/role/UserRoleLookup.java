package org.triplea.db.dao.user.role;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jdbi.v3.core.mapper.reflect.ColumnName;

@Getter
@EqualsAndHashCode
public class UserRoleLookup {
  private final int userId;
  private final int userRoleId;

  @Builder
  public UserRoleLookup(
      @ColumnName("id") final int userId, @ColumnName("user_role_id") final int userRoleId) {
    this.userId = userId;
    this.userRoleId = userRoleId;
  }
}
