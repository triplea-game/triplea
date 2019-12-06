package org.triplea.lobby.server.db.dao.username.ban;

import static org.triplea.lobby.server.db.dao.username.ban.UsernameBanRecord.DATE_CREATED_COLUMN;
import static org.triplea.lobby.server.db.dao.username.ban.UsernameBanRecord.USERNAME_COLUMN;

import java.util.List;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

/** Interface with the banned_names table, these are exact match names not allowed in the lobby. */
public interface UsernameBanDao {

  @SqlQuery(
      "select "
          + USERNAME_COLUMN
          + ", "
          + DATE_CREATED_COLUMN
          + "\n"
          + "from banned_username\n"
          + "order by username asc")
  List<UsernameBanRecord> getBannedUserNames();

  @SqlUpdate(
      "insert into banned_username(username)\n"
          + "values(:nameToBan)\n"
          + "on conflict(username) do nothing")
  int addBannedUserName(@Bind("nameToBan") String nameToBan);

  @SqlUpdate("delete from banned_username where username = :nameToRemove")
  int removeBannedUserName(@Bind("nameToRemove") String nameToRemove);
}
