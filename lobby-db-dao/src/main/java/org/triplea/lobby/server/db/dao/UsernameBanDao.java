package org.triplea.lobby.server.db.dao;

import static org.triplea.lobby.server.db.data.UsernameBanDaoData.DATE_CREATED_COLUMN;
import static org.triplea.lobby.server.db.data.UsernameBanDaoData.USERNAME_COLUMN;

import java.util.List;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.triplea.lobby.server.db.data.UsernameBanDaoData;


/**
 * Interface with the banned_names table, these are exact match names not allowed in the lobby.
 */
public interface UsernameBanDao {

  @SqlQuery("select "
      + USERNAME_COLUMN + ", "
      + DATE_CREATED_COLUMN + "\n"
      + "from banned_usernames\n"
      + "order by username")
  List<UsernameBanDaoData> getBannedUserNames();

  @SqlUpdate("insert into banned_usernames(username)\n"
      + "values(:nameToBan)\n"
      + "on conflict(username) do nothing")
  int addBannedUserName(@Bind("nameToBan") String nameToBan);

  @SqlUpdate("delete from banned_usernames where username = :nameToRemove")
  int removeBannedUserName(@Bind("nameToRemove") String nameToRemove);
}
