package org.triplea.lobby.server.db.dao;

import static org.triplea.lobby.server.db.data.ModeratorUserDaoData.LAST_LOGIN_COLUMN;
import static org.triplea.lobby.server.db.data.ModeratorUserDaoData.USERNAME_COLUMN;

import java.util.List;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.triplea.lobby.server.db.data.ModeratorUserDaoData;
import org.triplea.lobby.server.db.data.UserRole;

/** DAO for managing moderator users. */
public interface ModeratorsDao {
  @SqlQuery(
      "select "
          + "lu.username as "
          + USERNAME_COLUMN
          + ", lu.last_login as "
          + LAST_LOGIN_COLUMN
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
