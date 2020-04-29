package org.triplea.db.dao.moderator;

import java.util.List;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.triplea.db.dao.user.role.UserRole;

/** DAO for managing moderator users. */
public interface ModeratorsDao {
  @SqlQuery(
      "select "
          + "lu.username as "
          + ModeratorUserDaoData.USERNAME_COLUMN
          + ", lu.last_login as "
          + ModeratorUserDaoData.LAST_LOGIN_COLUMN
          + "\n"
          + "from lobby_user lu\n"
          + "join user_role ur on ur.id = lu.user_role_id\n"
          + "where ur.name in ('"
          + UserRole.MODERATOR
          + "', '"
          + UserRole.ADMIN
          + "')\n"
          + "order by username")
  List<ModeratorUserDaoData> getModerators();

  @SqlUpdate(
      "update lobby_user "
          + "set user_role_id = (select id from user_role where name = :role) "
          + "where id = :userId")
  int setRole(@Bind("userId") int userId, @Bind("role") String role);
}
