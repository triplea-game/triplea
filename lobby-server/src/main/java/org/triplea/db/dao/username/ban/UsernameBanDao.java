package org.triplea.db.dao.username.ban;

import java.util.List;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

/** Interface with the banned_names table, these are exact match names not allowed in the lobby. */
public interface UsernameBanDao {

  @SqlQuery(
      "select"
          + "    username,"
          + "    date_created"
          + "  from banned_username"
          + "  order by username asc")
  List<UsernameBanRecord> getBannedUserNames();

  @SqlUpdate(
      "insert into banned_username(username)\n"
          + "values(:nameToBan)\n"
          + "on conflict(username) do nothing")
  int addBannedUserName(@Bind("nameToBan") String nameToBan);

  @SqlUpdate("delete from banned_username where username = :nameToRemove")
  int removeBannedUserName(@Bind("nameToRemove") String nameToRemove);

  @SqlQuery(
      "select exists ( "
          + "select * "
          + "from banned_username "
          + "where username = lower(:playerName))")
  boolean nameIsBanned(@Bind("playerName") String playerName);
}
