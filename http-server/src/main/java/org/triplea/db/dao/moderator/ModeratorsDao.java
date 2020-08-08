package org.triplea.db.dao.moderator;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindList;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.triplea.db.dao.user.role.UserRole;

/** DAO for managing moderator users. */
public interface ModeratorsDao {
  @SqlQuery(
      "select "
          + "    lu.username,"
          + "    max(al.access_time) access_time"
          + "  from lobby_user lu"
          + "  left join access_log al on al.lobby_user_id = lu.id"
          + "  join user_role ur on ur.id = lu.user_role_id"
          + "  where ur.name in (<roles>)"
          + "  group by lu.username"
          + "  order by lu.username")
  List<ModeratorUserDaoData> getUserByRole(@BindList("roles") Collection<String> roles);

  default List<ModeratorUserDaoData> getModerators() {
    return getUserByRole(Set.of(UserRole.MODERATOR, UserRole.ADMIN));
  }

  @SqlUpdate(
      "update lobby_user"
          + "  set user_role_id = (select id from user_role where name = :role)"
          + "  where id = :userId")
  int setRole(@Bind("userId") int userId, @Bind("role") String role);
}
