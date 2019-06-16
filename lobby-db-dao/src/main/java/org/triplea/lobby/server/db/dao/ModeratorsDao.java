package org.triplea.lobby.server.db.dao;

import static org.triplea.lobby.server.db.data.ModeratorUserDaoData.LAST_LOGIN_COLUMN;
import static org.triplea.lobby.server.db.data.ModeratorUserDaoData.USERNAME_COLUMN;

import java.util.List;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.triplea.lobby.server.db.data.ModeratorUserDaoData;

/**
 * DAO for managing moderator users.
 */
public interface ModeratorsDao {
  @SqlQuery("select "
      + USERNAME_COLUMN + ", "
      + LAST_LOGIN_COLUMN + "\n"
      + "from lobby_user\n"
      + "where admin = true\n"
      + "order by username")
  List<ModeratorUserDaoData> getModerators();

  @SqlUpdate("update lobby_user set admin = false "
      + "where admin = true "
      + "  and super_mod = false "
      + "  and id = :userId")
  int removeMod(@Bind("userId") int userId);

  @SqlUpdate("update lobby_user set super_mod = true "
      + "where admin = true "
      + "  and super_mod = false"
      + "  and id = :userId")
  int addSuperMod(@Bind("userId") int userId);

  @SqlUpdate("update lobby_user set admin = true "
      + "where admin = false"
      + "  and id = :userId")
  int addMod(@Bind("userId") int userId);
}
