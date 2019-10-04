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
          + USERNAME_COLUMN
          + ", "
          + LAST_LOGIN_COLUMN
          + "\n"
          + "from lobby_user\n"
          + "where role = '"
          + UserRole.MODERATOR
          + "' or role = '"
          + UserRole.ADMIN
          + "'\n"
          + "order by username")
  List<ModeratorUserDaoData> getModerators();

  @SqlUpdate("update lobby_user set role = :role where id = :userId")
  int setRole(@Bind("userId") int userId, @Bind("role") String role);
}
