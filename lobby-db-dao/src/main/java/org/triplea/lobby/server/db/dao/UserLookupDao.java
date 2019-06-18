package org.triplea.lobby.server.db.dao;

import java.util.Optional;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;

/**
 * DAO for querying 'lobby_user' table.
 */
public interface UserLookupDao {
  @SqlQuery("select id from lobby_user where username = :username")
  Optional<Integer> lookupUserIdByName(@Bind("username") String username);
}
